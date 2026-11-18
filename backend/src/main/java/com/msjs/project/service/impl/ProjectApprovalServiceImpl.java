package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.ApprovalDeclareSelectionDTO;
import com.msjs.project.dto.ApprovalEditDTO;
import com.msjs.project.dto.ProjectApprovalSubmitDTO;
import com.msjs.project.dto.ProjectApprovalSubmitDTO.AttachmentItem;
import com.msjs.project.entity.Declaration;
import com.msjs.project.entity.DeclarationAttachment;
import com.msjs.project.entity.DeclarationHousehold;
import com.msjs.project.entity.DeclarationPublicFacility;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.ProjectApprovalAttachment;
import com.msjs.project.entity.ProjectApprovalDeclare;
import com.msjs.project.entity.ProjectApprovalDetail;
import com.msjs.project.mapper.DeclarationHouseholdMapper;
import com.msjs.project.mapper.DeclarationMapper;
import com.msjs.project.mapper.DeclarationPublicFacilityMapper;
import com.msjs.project.mapper.DeclarationAttachmentMapper;
import com.msjs.project.mapper.ProjectApprovalAttachmentMapper;
import com.msjs.project.mapper.ProjectApprovalDeclareMapper;
import com.msjs.project.mapper.ProjectApprovalDetailMapper;
import com.msjs.project.mapper.ProjectApprovalMapper;
import com.msjs.project.service.ProjectApprovalService;
import com.msjs.project.vo.ApprovalAttachmentVO;
import com.msjs.project.vo.ApprovalCountVO;
import com.msjs.project.vo.ApprovalDeclareVO;
import com.msjs.project.vo.ApprovalDetailVO;
import com.msjs.project.vo.ApprovalVO;
import com.msjs.project.vo.ProjectApprovalSubmitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 立项申请后端实现：
 *  - 事务：主表 + declare + detail + attachment 四张表原子写入
 *  - 事务：把选中 declaration 下的 approved 明细 audit_status → established
 *  - 事务：autoPromoteParentStatus（已在 DeclarationServiceImpl 里，这里直接复用其规则）
 * 附件不复用申报的附件表，用独立的 t_project_approval_attachment（bucket 默认 archive-formal）。
 * 立项状态统一使用 Constants.APPROVAL_STATUS_* 常量，避免大小写不一致导致页签筛选失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectApprovalServiceImpl implements ProjectApprovalService {

    private final ProjectApprovalMapper approvalMapper;
    private final ProjectApprovalDeclareMapper declareMapper;
    private final ProjectApprovalDetailMapper detailMapper;
    private final ProjectApprovalAttachmentMapper attachmentMapper;
    private final DeclarationMapper declarationMapper;
    private final DeclarationHouseholdMapper householdMapper;
    private final DeclarationPublicFacilityMapper facilityMapper;
    private final DeclarationAttachmentMapper declarationAttachmentMapper;

    private static final String DEFAULT_BUCKET = "archive-formal";

    // ==================== 提交立项 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectApprovalSubmitVO submit(ProjectApprovalSubmitDTO dto) {
        LoginUser me = UserContext.get();
        String username = me != null ? me.getUsername() : "system";
        Long userId = me != null ? me.getUserId() : null;
        LocalDateTime now = LocalDateTime.now();

        // 1. 立项编号唯一校验
        Long exists = approvalMapper.selectCount(
                new LambdaQueryWrapper<ProjectApproval>().eq(ProjectApproval::getApprovalCode, dto.getApprovalCode()));
        if (exists != null && exists > 0) {
            throw new BizException("立项编号已存在：" + dto.getApprovalCode());
        }

        // 2. 校验所选申报单（必须都是 createBy=当前人 或 recpientUserId=当前登录人，且状态为 submitted/approved/rejected）
        // 同一申报单允许按三级明细拆分成多次立项，这里按「申报单 + 选中明细」组合处理
        List<ApprovalDeclareSelectionDTO> selections = dto.getDeclarations();
        List<Long> declarationIds = selections.stream()
                .map(ApprovalDeclareSelectionDTO::getDeclarationId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (declarationIds.isEmpty() || declarationIds.size() != selections.size()) {
            throw new BizException("关联申报单不能为空且不能重复，请刷新后重试");
        }
        List<Declaration> parentList = declarationMapper.selectBatchIds(declarationIds);
        if (parentList == null || parentList.size() != declarationIds.size()) {
            throw new BizException("未找到所选的申报单");
        }
        Map<Long, List<Long>> detailIdsByDeclaration = selections.stream()
                .collect(Collectors.toMap(ApprovalDeclareSelectionDTO::getDeclarationId,
                        item -> item.getDetailIds() == null ? Collections.emptyList() : item.getDetailIds(),
                        (a, b) -> a));
        for (Declaration d : parentList) {
            boolean owned = username.equals(d.getCreateBy())
                    || (userId != null && d.getRecipientUserId() != null && userId.equals(d.getRecipientUserId()));
            if (!owned) {
                throw new BizException("申报单 " + d.getTitle() + " 不属于你");
            }
        }

        // 3. 立项主表
        ProjectApproval approval = new ProjectApproval();
        approval.setApprovalCode(dto.getApprovalCode());
        approval.setProjectName(dto.getProjectName());
        approval.setProjectCategory(dto.getProjectCategory());
        approval.setProjectType(dto.getProjectType());
        approval.setProjectSource(dto.getProjectSource());
        approval.setBudget(dto.getBudget());
        approval.setLeader(dto.getLeader());
        approval.setApplyBy(dto.getApplyBy());
        approval.setRemark(dto.getRemark());
        approval.setStatus(Constants.APPROVAL_STATUS_PENDING);
        // 立项日期优先用前端传入的 establishDate（以 00:00:00 存储为 submitTime），其次用 submitTime，最后用 now
        LocalDateTime submitAt;
        if (dto.getSubmitTime() != null) {
            submitAt = dto.getSubmitTime();
        } else if (dto.getEstablishDate() != null) {
            submitAt = dto.getEstablishDate().atStartOfDay();
        } else {
            submitAt = now;
        }
        approval.setSubmitTime(submitAt);
        approval.setRegionCode(dto.getTownCode());
        approval.setRegionName(dto.getTownName());
        approval.setCreateBy(username);
        approval.setCreateTime(now);
        approval.setUpdateBy(username);
        approval.setUpdateTime(now);
        approvalMapper.insert(approval);

        // 3.1 不再对"同申报单已被其他 PENDING 立项绑定"做自动作废，
        //     业务上允许一条申报单同时出现在多条立项单中（可按明细分批立项）。
        //     evictFromPendingApproval 方法保留给后续若有需要的手动作废入口调用。

        // 4. declare + detail + 状态推进（按前端选中的明细绑定，未选明细时绑定该申报单下全部已通过明细）
        int declareCount = 0, detailCount = 0;
        for (Long declarationId : declarationIds) {
            Declaration d = parentList.stream()
                    .filter(item -> declarationId.equals(item.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BizException("未找到所选的申报单"));
            // bindDeclaration 内部已完成：declare 创建 + 选中明细立项 + 状态回算
            detailCount += bindDeclaration(approval, d, declareCount,
                    detailIdsByDeclaration.get(declarationId), userId, username, now);
            declareCount++;
        }

        // 5. 附件行（项目级；declareId 为空，挂在 approval 下）
        int attachmentCount = 0;
        if (dto.getAttachments() != null) {
            for (AttachmentItem a : dto.getAttachments()) {
                if (a.getFileKey() == null || a.getFileKey().isBlank()) {
                    continue;
                }
                ProjectApprovalAttachment pa = new ProjectApprovalAttachment();
                pa.setApprovalId(approval.getId());
                pa.setAttachmentType(a.getAttachmentType());
                pa.setFileName(a.getFileName());
                pa.setFileKey(a.getFileKey());
                pa.setBucket(a.getBucket() != null && !a.getBucket().isBlank() ? a.getBucket() : DEFAULT_BUCKET);
                pa.setFileSize(a.getFileSize());
                pa.setRemark(a.getRemark());
                pa.setCreateTime(now);
                attachmentMapper.insert(pa);
                attachmentCount++;
            }
        }

        // 6. 校验：至少要有一条 approved 明细被立项，否则不给过
        if (detailCount == 0) {
            throw new BizException("所选申报单下没有已通过的明细可立项");
        }

        ProjectApprovalSubmitVO vo = new ProjectApprovalSubmitVO();
        vo.setId(approval.getId());
        vo.setApprovalCode(approval.getApprovalCode());
        vo.setStatus(approval.getStatus());
        vo.setDeclareCount(declareCount);
        vo.setDetailCount(detailCount);
        vo.setAttachmentCount(attachmentCount);
        return vo;
    }

    // ==================== 分页 / 统计 / 详情 ====================

    @Override
    public PageVO<ApprovalVO> page(long pageNum, long pageSize, String tab, String projectName,
                                   String projectCategory, String projectType, String startTime, String endTime) {
        LambdaQueryWrapper<ProjectApproval> wrapper = buildFilterWrapper(
                projectName, projectCategory, projectType, startTime, endTime);
        // 页签仅支持待审核 / 已驳回，状态值统一使用常量
        if (Constants.APPROVAL_TAB_PENDING.equals(tab)) {
            wrapper.eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_PENDING);
        } else if (Constants.APPROVAL_TAB_REJECTED.equals(tab)) {
            wrapper.eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_REJECTED);
        } else {
            throw new BizException("不支持的页签类型：" + tab);
        }
        wrapper.orderByDesc(ProjectApproval::getSubmitTime).orderByDesc(ProjectApproval::getId);

        Page<ProjectApproval> result = approvalMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        PageVO<ApprovalVO> vo = new PageVO<>();
        vo.setRecords(buildTree(result.getRecords()));
        vo.setTotal(result.getTotal());
        vo.setPageNum(result.getCurrent());
        vo.setPageSize(result.getSize());
        return vo;
    }

    @Override
    public ApprovalCountVO counts(String projectName, String projectCategory, String projectType,
                                  String startTime, String endTime) {
        ApprovalCountVO vo = new ApprovalCountVO();
        vo.setAll(approvalMapper.selectCount(buildFilterWrapper(projectName, projectCategory, projectType, startTime, endTime)));
        vo.setPending(approvalMapper.selectCount(buildFilterWrapper(projectName, projectCategory, projectType, startTime, endTime)
                .eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_PENDING)));
        vo.setRejected(approvalMapper.selectCount(buildFilterWrapper(projectName, projectCategory, projectType, startTime, endTime)
                .eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_REJECTED)));
        return vo;
    }

    @Override
    public ApprovalVO detail(Long id) {
        ProjectApproval approval = requireApproval(id);
        return buildTree(List.of(approval)).get(0);
    }

    @Override
    public List<ApprovalAttachmentVO> attachments(Long approvalId) {
        requireApproval(approvalId);
        List<ProjectApprovalAttachment> list = attachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalAttachment>()
                        .eq(ProjectApprovalAttachment::getApprovalId, approvalId)
                        .orderByAsc(ProjectApprovalAttachment::getId));
        return list.stream().map(this::toAttachmentVO).toList();
    }

    // ==================== 编辑 / 审核 / 退回 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(ApprovalEditDTO dto) {
        ProjectApproval approval = requireApproval(dto.getId());
        // 业务约束：仅待审核状态可编辑（equalsIgnoreCase 兼容历史数据大小写）
        if (!Constants.APPROVAL_STATUS_PENDING.equalsIgnoreCase(approval.getStatus())) {
            throw new BizException("仅待审核的立项可编辑");
        }
        approval.setProjectName(trimToNull(dto.getProjectName()));
        approval.setProjectCategory(trimToNull(dto.getProjectCategory()));
        approval.setProjectType(trimToNull(dto.getProjectType()));
        approval.setProjectSource(trimToNull(dto.getProjectSource()));
        approval.setBudget(dto.getBudget());
        approval.setLeader(trimToNull(dto.getLeader()));
        approval.setApplyBy(trimToNull(dto.getApplyBy()));
        approval.setRemark(trimToNull(dto.getRemark()));
        approval.setUpdateBy(currentUser());
        approval.setUpdateTime(LocalDateTime.now());
        approvalMapper.updateById(approval);

        saveProjectAttachments(approval.getId(), dto.getAttachments());

        // 先移除关联申报单：删除 declare + 明细，并把申报明细状态回退为已通过
        // 再新增关联申报单：避免「移除后又重新按其他明细关联同一申报单」时命中同立项重复校验
        if (dto.getRemoveDeclarationIds() != null && !dto.getRemoveDeclarationIds().isEmpty()) {
            unbindDeclarations(approval, dto.getRemoveDeclarationIds());
        }
        if (dto.getAddDeclarations() != null && !dto.getAddDeclarations().isEmpty()) {
            bindNewDeclarations(approval, dto.getAddDeclarations());
        }
    }

    /**
     * 若某 declarationId 已被**其他非终态审批单**绑定（PENDING 或类似待审状态），
     * 则自动作废旧审批单 + 硬删 declare/detail/attachment + 明细状态回退为 approved。
     * —— 目的：允许审批人重新发起同一申报单的立项，不会撞 declare_code 唯一键。
     * （终态 APPROVED/REJECTED 不动，防止篡改已归档审批结果）
     */
    private void evictFromPendingApproval(Long declarationId, String businessCode, String username, LocalDateTime now) {
        if (declarationId == null) return;
        List<ProjectApprovalDeclare> linked = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .eq(ProjectApprovalDeclare::getDeclarationId, declarationId));
        if (linked.isEmpty()) return;

        // 同 declarationId 可能被多次绑定，取最近那条（sort/更新时间最大）对应的 approval
        for (ProjectApprovalDeclare declare : linked) {
            ProjectApproval oldApproval = approvalMapper.selectById(declare.getApprovalId());
            if (oldApproval == null) continue;
            if (Constants.APPROVAL_STATUS_APPROVED.equals(oldApproval.getStatus())
                    || Constants.APPROVAL_STATUS_REJECTED.equals(oldApproval.getStatus())) {
                continue; // 终态不动
            }
            // 作废旧审批单
            oldApproval.setStatus(Constants.APPROVAL_STATUS_REJECTED);
            oldApproval.setRemark("作废（由系统自动处理：申报单 " + businessCode
                    + " 被重新立项，旧审批单 " + oldApproval.getApprovalCode() + " 自动作废）");
            oldApproval.setUpdateBy(username);
            oldApproval.setUpdateTime(now);
            approvalMapper.updateById(oldApproval);

            // 复用 unbindDeclarations 的解绑逻辑（当前 approval 无所谓，只拿 declare 链）
            unbindDeclarations(oldApproval, List.of(declarationId));
        }
    }

    /** 编辑时移除关联申报单：删除申报节点及其明细，申报明细状态 established → approved */
    private void unbindDeclarations(ProjectApproval approval, List<Long> removeIds) {
        List<ProjectApprovalDeclare> linked = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .eq(ProjectApprovalDeclare::getApprovalId, approval.getId())
                        .in(ProjectApprovalDeclare::getDeclarationId, removeIds));
        if (linked.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ProjectApprovalDeclare declare : linked) {
            List<ProjectApprovalDetail> details = detailMapper.selectList(
                    new LambdaQueryWrapper<ProjectApprovalDetail>()
                            .eq(ProjectApprovalDetail::getDeclareId, declare.getId()));
            // 明细状态回退：已立项 → 已通过，便于重新选择申报单
            for (ProjectApprovalDetail detail : details) {
                if (Constants.APPROVAL_DETAIL_TYPE_HOUSEHOLD.equals(detail.getDetailType())
                        && detail.getHouseholdId() != null) {
                    DeclarationHousehold upd = new DeclarationHousehold();
                    upd.setId(detail.getHouseholdId());
                    upd.setAuditStatus("approved");
                    upd.setAuditTime(now);
                    upd.setAuditRemark("已移除立项（审批单：" + approval.getApprovalCode() + "）");
                    householdMapper.updateById(upd);
                } else if (Constants.APPROVAL_DETAIL_TYPE_FACILITY.equals(detail.getDetailType())
                        && detail.getFacilityId() != null) {
                    DeclarationPublicFacility upd = new DeclarationPublicFacility();
                    upd.setId(detail.getFacilityId());
                    upd.setAuditStatus("approved");
                    upd.setAuditTime(now);
                    upd.setAuditRemark("已移除立项（审批单：" + approval.getApprovalCode() + "）");
                    facilityMapper.updateById(upd);
                }
            }
            if (!details.isEmpty()) {
                detailMapper.deleteBatchIds(details.stream().map(ProjectApprovalDetail::getId).toList());
            }
            // 清理该申报节点下挂的申报材料附件，避免孤儿数据
            attachmentMapper.delete(new LambdaQueryWrapper<ProjectApprovalAttachment>()
                    .eq(ProjectApprovalAttachment::getApprovalId, approval.getId())
                    .eq(ProjectApprovalAttachment::getDeclareId, declare.getId()));
            // 物理删除关联行：逻辑删除会残留 declare_code 唯一索引占位，导致该申报单无法重新关联
            declareMapper.hardDeleteById(declare.getId());
        }
    }

    /** 编辑时追加关联申报单：去重校验后按选中明细逐条绑定 */
    private void bindNewDeclarations(ProjectApproval approval, List<ApprovalDeclareSelectionDTO> addSelections) {
        LoginUser me = UserContext.get();
        String username = me != null ? me.getUsername() : "system";
        Long userId = me != null ? me.getUserId() : null;
        LocalDateTime now = LocalDateTime.now();

        // 已绑定的申报单集合，避免同一立项内重复挂接（跨立项允许同一申报单按明细拆分）
        List<ProjectApprovalDeclare> existing = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .eq(ProjectApprovalDeclare::getApprovalId, approval.getId()));
        Set<Long> linkedIds = existing.stream()
                .map(ProjectApprovalDeclare::getDeclarationId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<Long> distinctIds = addSelections.stream()
                .map(ApprovalDeclareSelectionDTO::getDeclarationId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty() || distinctIds.size() != addSelections.size()) {
            throw new BizException("新增申报单不能为空且不能重复，请刷新后重试");
        }
        Map<Long, List<Long>> detailIdsByDeclaration = addSelections.stream()
                .collect(Collectors.toMap(ApprovalDeclareSelectionDTO::getDeclarationId,
                        item -> item.getDetailIds() == null ? Collections.emptyList() : item.getDetailIds(),
                        (a, b) -> a));
        List<Declaration> declarations = declarationMapper.selectBatchIds(distinctIds);
        if (declarations == null || declarations.size() != distinctIds.size()) {
            throw new BizException("部分申报单不存在，请刷新后重试");
        }

        int sort = existing.size();
        for (Declaration d : declarations) {
            if (linkedIds.contains(d.getId())) {
                throw new BizException("申报单「" + d.getTitle() + "」已关联，请勿重复添加");
            }
            int detailCount = bindDeclaration(approval, d, sort++,
                    detailIdsByDeclaration.get(d.getId()), userId, username, now);
            if (detailCount == 0) {
                throw new BizException("申报单「" + d.getTitle() + "」下没有已通过的明细可立项");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        changeStatus(id, Constants.APPROVAL_STATUS_PENDING, Constants.APPROVAL_STATUS_APPROVED,
                "仅待审核的立项可审核通过");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id) {
        changeStatus(id, Constants.APPROVAL_STATUS_PENDING, Constants.APPROVAL_STATUS_REJECTED,
                "仅待审核的立项可驳回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnToPending(Long id) {
        changeStatus(id, Constants.APPROVAL_STATUS_REJECTED, Constants.APPROVAL_STATUS_PENDING,
                "仅已驳回的立项可退回待审核");
    }

    /** 状态流转：校验当前状态后仅更新状态与更新人/更新时间 */
    private void changeStatus(Long id, String fromStatus, String toStatus, String errorMsg) {
        ProjectApproval approval = requireApproval(id);
        // 使用 equalsIgnoreCase 防御存量数据 status 大小写不一致的问题
        if (!fromStatus.equalsIgnoreCase(approval.getStatus())) {
            throw new BizException(errorMsg);
        }
        ProjectApproval update = new ProjectApproval();
        update.setId(approval.getId());
        update.setStatus(toStatus);
        // 审核通过后项目即进入验收管理（与资金管理、施工管理同源），显式初始化验收状态，
        // 避免历史数据 accept_status 为空导致验收列表查不到
        if (Constants.APPROVAL_STATUS_APPROVED.equals(toStatus)) {
            update.setAcceptStatus(Constants.APPROVAL_ACCEPT_NONE);
        }
        update.setUpdateBy(currentUser());
        update.setUpdateTime(LocalDateTime.now());
        approvalMapper.updateById(update);
    }

    // ==================== 树形组装 ====================

    /**
     * 批量组装三级树（项目 → 申报内容 → 明细），避免 N+1 查询
     */
    private List<ApprovalVO> buildTree(List<ProjectApproval> approvals) {
        if (approvals == null || approvals.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> approvalIds = approvals.stream().map(ProjectApproval::getId).toList();
        List<ProjectApprovalDeclare> declares = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .in(ProjectApprovalDeclare::getApprovalId, approvalIds)
                        .orderByAsc(ProjectApprovalDeclare::getSort)
                        .orderByAsc(ProjectApprovalDeclare::getId));

        List<ProjectApprovalDetail> details = new ArrayList<>();
        if (!declares.isEmpty()) {
            List<Long> declareIds = declares.stream().map(ProjectApprovalDeclare::getId).toList();
            details = detailMapper.selectList(new LambdaQueryWrapper<ProjectApprovalDetail>()
                    .in(ProjectApprovalDetail::getDeclareId, declareIds)
                    .orderByAsc(ProjectApprovalDetail::getSort)
                    .orderByAsc(ProjectApprovalDetail::getId));
        }

        // 申报材料附件（declare_id 非空）随申报节点返回；项目级附件由 /attachments 接口单独查询
        List<ProjectApprovalAttachment> declareAttachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalAttachment>()
                        .in(ProjectApprovalAttachment::getApprovalId, approvalIds)
                        .isNotNull(ProjectApprovalAttachment::getDeclareId)
                        .orderByAsc(ProjectApprovalAttachment::getId));

        Map<Long, List<ProjectApprovalDetail>> detailsByDeclare = details.stream()
                .collect(Collectors.groupingBy(ProjectApprovalDetail::getDeclareId));
        Map<Long, List<ProjectApprovalDeclare>> declaresByApproval = declares.stream()
                .collect(Collectors.groupingBy(ProjectApprovalDeclare::getApprovalId));
        Map<Long, List<ApprovalAttachmentVO>> attachmentsByDeclare = declareAttachments.stream()
                .collect(Collectors.groupingBy(ProjectApprovalAttachment::getDeclareId,
                        Collectors.mapping(this::toAttachmentVO, Collectors.toList())));
        // 明细自身附件：取申报模块中挂在农户/公建明细行上的附件，而非申报单附件
        Map<String, List<ApprovalAttachmentVO>> detailAttachments = loadDetailAttachments(details);

        List<ApprovalVO> records = new ArrayList<>(approvals.size());
        for (ProjectApproval approval : approvals) {
            ApprovalVO projectNode = toApprovalVO(approval);
            List<ApprovalDeclareVO> declareNodes = new ArrayList<>();
            for (ProjectApprovalDeclare declare : declaresByApproval.getOrDefault(approval.getId(), Collections.emptyList())) {
                ApprovalDeclareVO declareNode = toDeclareVO(declare);
                declareNode.setChildren(detailsByDeclare
                        .getOrDefault(declare.getId(), Collections.emptyList())
                        .stream().map(detail -> toDetailVO(detail, detailAttachments)).toList());
                declareNode.setAttachments(attachmentsByDeclare.getOrDefault(declare.getId(), Collections.emptyList()));
                declareNodes.add(declareNode);
            }
            projectNode.setChildren(declareNodes);
            records.add(projectNode);
        }
        return records;
    }

    private ApprovalVO toApprovalVO(ProjectApproval approval) {
        ApprovalVO vo = new ApprovalVO();
        vo.setId(approval.getId());
        vo.setApprovalCode(approval.getApprovalCode());
        vo.setProjectName(approval.getProjectName());
        vo.setProjectCategory(approval.getProjectCategory());
        vo.setProjectType(approval.getProjectType());
        vo.setProjectSource(approval.getProjectSource());
        vo.setBudget(approval.getBudget());
        vo.setLeader(approval.getLeader());
        vo.setApplyBy(approval.getApplyBy());
        vo.setSubmitTime(approval.getSubmitTime());
        vo.setStatus(approval.getStatus());
        vo.setRegionCode(approval.getRegionCode());
        vo.setRegionName(approval.getRegionName());
        vo.setRemark(approval.getRemark());
        vo.setCreateBy(approval.getCreateBy());
        vo.setCreateTime(approval.getCreateTime());
        return vo;
    }

    private ApprovalDeclareVO toDeclareVO(ProjectApprovalDeclare declare) {
        ApprovalDeclareVO vo = new ApprovalDeclareVO();
        vo.setId(declare.getId());
        vo.setDeclarationId(declare.getDeclarationId());
        vo.setApprovalId(declare.getApprovalId());
        vo.setDeclareCode(declare.getDeclareCode());
        vo.setDeclareName(declare.getDeclareName());
        vo.setDeclareCategory(declare.getDeclareCategory());
        vo.setDeclareType(declare.getDeclareType());
        vo.setTownName(declare.getTownName());
        vo.setVillageName(declare.getVillageName());
        vo.setDeclareContent(declare.getDeclareContent());
        vo.setBudget(declare.getBudget());
        vo.setSort(declare.getSort());
        return vo;
    }

    private ApprovalDetailVO toDetailVO(ProjectApprovalDetail detail,
                                        Map<String, List<ApprovalAttachmentVO>> detailAttachments) {
        ApprovalDetailVO vo = new ApprovalDetailVO();
        vo.setId(detail.getId());
        vo.setDeclareId(detail.getDeclareId());
        vo.setDetailType(normalizeDetailType(detail.getDetailType()));
        vo.setTargetId(detail.getTargetId());
        vo.setTargetName(detail.getTargetName());
        vo.setTargetCode(detail.getTargetCode());
        vo.setRegionName(detail.getRegionName());
        vo.setAmount(detail.getAmount());
        vo.setQuantity(detail.getQuantity());
        vo.setRemark(detail.getRemark());
        vo.setSort(detail.getSort());
        // 明细附件按明细类型取对应归属行的附件：农户 → household，公建 → public_facility
        boolean facility = Constants.APPROVAL_DETAIL_TYPE_FACILITY.equals(vo.getDetailType());
        Long ownerId = facility ? detail.getFacilityId() : detail.getHouseholdId();
        String ownerType = facility ? "public_facility" : "household";
        vo.setAttachments(ownerId == null ? Collections.emptyList()
                : detailAttachments.getOrDefault(ownerType + "#" + ownerId, Collections.emptyList()));
        return vo;
    }

    /**
     * 批量加载明细自身附件。
     * 业务约束：农户/公建明细的附件存放在申报附件表 t_declaration_attachment，
     * owner_type = household / public_facility，owner_id = 明细对应的申报农户/公建行 ID。
     */
    private Map<String, List<ApprovalAttachmentVO>> loadDetailAttachments(List<ProjectApprovalDetail> details) {
        Set<Long> householdIds = new HashSet<>();
        Set<Long> facilityIds = new HashSet<>();
        for (ProjectApprovalDetail detail : details) {
            if (Constants.APPROVAL_DETAIL_TYPE_FACILITY.equals(normalizeDetailType(detail.getDetailType()))) {
                if (detail.getFacilityId() != null) {
                    facilityIds.add(detail.getFacilityId());
                }
            } else if (detail.getHouseholdId() != null) {
                householdIds.add(detail.getHouseholdId());
            }
        }
        Map<String, List<ApprovalAttachmentVO>> result = new java.util.HashMap<>();
        collectDetailAttachments("household", householdIds, result);
        collectDetailAttachments("public_facility", facilityIds, result);
        return result;
    }

    /** 按归属类型批量查询明细附件，key = ownerType#ownerId */
    private void collectDetailAttachments(String ownerType, Set<Long> ownerIds,
                                          Map<String, List<ApprovalAttachmentVO>> result) {
        if (ownerIds.isEmpty()) {
            return;
        }
        List<DeclarationAttachment> list = declarationAttachmentMapper.selectList(
                new LambdaQueryWrapper<DeclarationAttachment>()
                        .eq(DeclarationAttachment::getOwnerType, ownerType)
                        .in(DeclarationAttachment::getOwnerId, ownerIds)
                        .orderByAsc(DeclarationAttachment::getId));
        for (DeclarationAttachment item : list) {
            ApprovalAttachmentVO vo = new ApprovalAttachmentVO();
            vo.setId(item.getId());
            vo.setAttachmentType(item.getAttachmentType());
            vo.setFileName(item.getFileName());
            vo.setFileKey(item.getFileKey());
            vo.setBucket(item.getBucket());
            vo.setFileSize(item.getFileSize());
            vo.setRemark(item.getRemark());
            vo.setCreateTime(item.getCreateTime());
            result.computeIfAbsent(ownerType + "#" + item.getOwnerId(), k -> new ArrayList<>()).add(vo);
        }
    }

    private ApprovalAttachmentVO toAttachmentVO(ProjectApprovalAttachment attachment) {
        ApprovalAttachmentVO vo = new ApprovalAttachmentVO();
        vo.setId(attachment.getId());
        vo.setApprovalId(attachment.getApprovalId());
        vo.setDeclareId(attachment.getDeclareId());
        vo.setAttachmentType(attachment.getAttachmentType());
        vo.setFileName(attachment.getFileName());
        vo.setFileKey(attachment.getFileKey());
        vo.setBucket(attachment.getBucket());
        vo.setFileSize(attachment.getFileSize());
        vo.setRemark(attachment.getRemark());
        vo.setCreateTime(attachment.getCreateTime());
        return vo;
    }

    /**
     * 明细类型兼容：早期数据使用 PUBLIC_FACILITY，统一对外输出 FACILITY（与前端类型一致）
     */
    private String normalizeDetailType(String detailType) {
        if ("PUBLIC_FACILITY".equals(detailType)) {
            return Constants.APPROVAL_DETAIL_TYPE_FACILITY;
        }
        return detailType;
    }

    // ==================== 附件维护 ====================

    /**
     * 同步项目级立项资料附件（declare_id 为空）：
     * 已存在附件按 ID 差异更新，本次未提交的已有附件逻辑删除，新增附件直接插入；
     * 申报材料附件（declare_id 非空）由申报模块维护，此处忽略，避免串改。
     */
    private void saveProjectAttachments(Long approvalId, List<ApprovalEditDTO.AttachmentItem> attachments) {
        if (attachments == null) {
            // 未传附件列表视为不修改附件
            return;
        }
        List<ProjectApprovalAttachment> existList = attachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalAttachment>()
                        .eq(ProjectApprovalAttachment::getApprovalId, approvalId)
                        .isNull(ProjectApprovalAttachment::getDeclareId));
        Map<Long, ProjectApprovalAttachment> existMap = existList.stream()
                .collect(Collectors.toMap(ProjectApprovalAttachment::getId, item -> item, (a, b) -> a));

        Set<Long> inputIds = new HashSet<>();
        for (ApprovalEditDTO.AttachmentItem item : attachments) {
            if (item.getId() != null && existMap.containsKey(item.getId())) {
                inputIds.add(item.getId());
            }
        }
        for (ProjectApprovalAttachment exist : existList) {
            if (!inputIds.contains(exist.getId())) {
                attachmentMapper.deleteById(exist.getId());
            }
        }
        for (ApprovalEditDTO.AttachmentItem item : attachments) {
            if (item.getId() != null) {
                ProjectApprovalAttachment exist = existMap.get(item.getId());
                if (exist != null) {
                    fillAttachment(exist, item);
                    attachmentMapper.updateById(exist);
                }
                continue;
            }
            // 新增附件必须带文件 key，否则不落库
            if (item.getFileKey() == null || item.getFileKey().isBlank()) {
                continue;
            }
            ProjectApprovalAttachment attachment = new ProjectApprovalAttachment();
            attachment.setApprovalId(approvalId);
            fillAttachment(attachment, item);
            attachment.setCreateTime(LocalDateTime.now());
            attachment.setDeleted(0);
            attachmentMapper.insert(attachment);
        }
    }

    private void fillAttachment(ProjectApprovalAttachment attachment, ApprovalEditDTO.AttachmentItem item) {
        attachment.setAttachmentType(trimToNull(item.getAttachmentType()));
        attachment.setFileName(trimToNull(item.getFileName()));
        attachment.setFileKey(trimToNull(item.getFileKey()));
        attachment.setBucket(trimToNull(item.getBucket()));
        attachment.setFileSize(item.getFileSize());
        attachment.setRemark(trimToNull(item.getRemark()));
    }

    // ==================== helpers ====================

    private ProjectApproval requireApproval(Long id) {
        if (id == null) {
            throw new BizException("立项ID不能为空");
        }
        ProjectApproval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BizException("立项信息不存在或已被删除");
        }
        return approval;
    }

    /** 构造列表筛选条件（项目名称/类别/类型/提交时间范围） */
    private LambdaQueryWrapper<ProjectApproval> buildFilterWrapper(String projectName, String projectCategory,
                                                                   String projectType, String startTime,
                                                                   String endTime) {
        LocalDate startDate = parseDate(startTime, "提交开始日期");
        LocalDate endDate = parseDate(endTime, "提交结束日期");
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime end = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        return new LambdaQueryWrapper<ProjectApproval>()
                .like(isNotBlank(projectName), ProjectApproval::getProjectName, trimToNull(projectName))
                .eq(isNotBlank(projectCategory), ProjectApproval::getProjectCategory, trimToNull(projectCategory))
                .eq(isNotBlank(projectType), ProjectApproval::getProjectType, trimToNull(projectType))
                .ge(start != null, ProjectApproval::getSubmitTime, start)
                .le(end != null, ProjectApproval::getSubmitTime, end);
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (!isNotBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BizException(fieldName + "格式不正确，应为 yyyy-MM-dd");
        }
    }

    /**
     * 将一条申报单（及本次选中的三级明细）绑定到立项单：
     * 1) 落一条 t_project_approval_declare（申报内容节点）
     * 2) 按 selectedDetailIds 抓 approved 明细 → 落 t_project_approval_detail
     *    selectedDetailIds 为空时绑定该申报单下全部 approved 明细
     * 3) 明细 audit_status 推进为 established，父申报单状态回算为 approved
     *
     * @return 本次落库的明细条数（0 表示该申报单下没有已通过明细）
     */
    private int bindDeclaration(ProjectApproval approval, Declaration d, int sort,
                                List<Long> selectedDetailIds, Long userId, String username, LocalDateTime now) {
        // 1. 申报内容节点
        ProjectApprovalDeclare declare = new ProjectApprovalDeclare();
        declare.setApprovalId(approval.getId());
        declare.setDeclarationId(d.getId());
        declare.setDeclareCode(d.getBusinessCode());
        declare.setDeclareName(d.getTitle());
        declare.setDeclareCategory(d.getDeclarationCategory());
        declare.setDeclareType(d.getDeclarationType());
        declare.setTownName(d.getTownName());
        declare.setVillageName(d.getVillageGroupName()); // 拷贝所属村组（之前漏掉，导致 declare.village_name 为空）
        declare.setBudget(d.getAmount());
        declare.setSort(sort);
        declare.setCreateTime(now);
        // 同一申报编号允许出现在多个立项下（按明细拆分立项），这里仍清理历史逻辑删除行，避免脏数据堆积
        declareMapper.hardDeleteSoftDeletedByCode(declare.getDeclareCode());
        declareMapper.insert(declare);

        // 2. 已通过明细 → 立项明细，并推进为已立项；选中明细为空时视为该申报单下全部已通过明细
        Set<Long> selectedIds = selectedDetailIds == null ? Collections.emptySet()
                : selectedDetailIds.stream().filter(java.util.Objects::nonNull)
                        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        int detailCount = 0;
        if ("1".equals(d.getDeclarationCategory())) {
            // 农户
            List<DeclarationHousehold> all = householdMapper.selectList(
                    new LambdaQueryWrapper<DeclarationHousehold>()
                            .eq(DeclarationHousehold::getDeclarationId, d.getId())
                            .eq(DeclarationHousehold::getDeleted, 0));
            Map<Long, DeclarationHousehold> detailMap = all.stream()
                    .collect(Collectors.toMap(DeclarationHousehold::getId, item -> item, (a, b) -> a));
            for (DeclarationHousehold hh : all) {
                if (!isDetailPicked(selectedIds, hh.getId())) {
                    continue;
                }
                if (!"approved".equals(hh.getAuditStatus())) {
                    throw new BizException("农户明细「" + hh.getSnapshotHeadName()
                            + "」当前状态为「" + auditStatusLabel(hh.getAuditStatus()) + "」，不能重复立项");
                }
                ProjectApprovalDetail pd = buildHouseholdDetail(declare.getId(), hh);
                pd.setDeclarationId(d.getId());
                detailMapper.insert(pd);
                // 改状态：approved → established
                DeclarationHousehold upd = new DeclarationHousehold();
                upd.setId(hh.getId());
                upd.setAuditStatus("established");
                upd.setAuditBy(userId);
                upd.setAuditTime(now);
                upd.setAuditRemark("已立项（审批单：" + approval.getApprovalCode() + "）");
                householdMapper.updateById(upd);
                detailCount++;
            }
            // 选中的明细必须都属于该申报单，避免跨申报单串数据
            for (Long detailId : selectedIds) {
                if (!detailMap.containsKey(detailId)) {
                    throw new BizException("所选农户明细不属于该申报单或已被删除，请刷新后重试");
                }
            }
        } else if ("2".equals(d.getDeclarationCategory())) {
            // 公建
            List<DeclarationPublicFacility> all = facilityMapper.selectList(
                    new LambdaQueryWrapper<DeclarationPublicFacility>()
                            .eq(DeclarationPublicFacility::getDeclarationId, d.getId())
                            .eq(DeclarationPublicFacility::getDeleted, 0));
            Map<Long, DeclarationPublicFacility> detailMap = all.stream()
                    .collect(Collectors.toMap(DeclarationPublicFacility::getId, item -> item, (a, b) -> a));
            for (DeclarationPublicFacility pf : all) {
                if (!isDetailPicked(selectedIds, pf.getId())) {
                    continue;
                }
                if (!"approved".equals(pf.getAuditStatus())) {
                    throw new BizException("公共基础设施明细「" + pf.getSnapshotFacilityName()
                            + "」当前状态为「" + auditStatusLabel(pf.getAuditStatus()) + "」，不能重复立项");
                }
                ProjectApprovalDetail pd = buildFacilityDetail(declare.getId(), pf);
                pd.setDeclarationId(d.getId());
                detailMapper.insert(pd);
                DeclarationPublicFacility upd = new DeclarationPublicFacility();
                upd.setId(pf.getId());
                upd.setAuditStatus("established");
                upd.setAuditBy(userId);
                upd.setAuditTime(now);
                upd.setAuditRemark("已立项（审批单：" + approval.getApprovalCode() + "）");
                facilityMapper.updateById(upd);
                detailCount++;
            }
            for (Long detailId : selectedIds) {
                if (!detailMap.containsKey(detailId)) {
                    throw new BizException("所选公共基础设施明细不属于该申报单或已被删除，请刷新后重试");
                }
            }
        }

        // 3. 父申报单状态回算（增量立项：只把 approved 明细推进成 established，其余 pending 明细不动）
        //    有 pending → 保持 submitted（不锁死剩余审核入口，审批人还能继续驳回/通过）
        //    无 pending 且无 rejected → 推 approved（全部完成）
        //    无 pending 但有 rejected → 推 rejected（整体驳回）
        boolean hasPending;
        boolean hasRejected;
        if ("1".equals(d.getDeclarationCategory())) {
            List<String> ss = householdMapper.selectList(new LambdaQueryWrapper<DeclarationHousehold>()
                            .eq(DeclarationHousehold::getDeclarationId, d.getId()))
                    .stream().map(DeclarationHousehold::getAuditStatus).toList();
            hasPending = ss.stream().anyMatch(s -> s == null || s.isEmpty() || "pending".equals(s));
            hasRejected = ss.stream().anyMatch("rejected"::equals);
        } else if ("2".equals(d.getDeclarationCategory())) {
            List<String> ss = facilityMapper.selectList(new LambdaQueryWrapper<DeclarationPublicFacility>()
                            .eq(DeclarationPublicFacility::getDeclarationId, d.getId()))
                    .stream().map(DeclarationPublicFacility::getAuditStatus).toList();
            hasPending = ss.stream().anyMatch(s -> s == null || s.isEmpty() || "pending".equals(s));
            hasRejected = ss.stream().anyMatch("rejected"::equals);
        } else {
            hasPending = false;
            hasRejected = false;
        }

        Declaration upd = new Declaration();
        upd.setId(d.getId());
        upd.setUpdateBy(username);
        upd.setUpdateTime(now);
        if (hasPending) {
            // 还有 pending：保持 submitted，让剩余明细还能驳回/通过
            upd.setStatus("submitted");
            upd.setAuditBy(userId);
            upd.setAuditTime(now);
            upd.setAuditRemark("部分明细已立项（审批单：" + approval.getApprovalCode() + "），剩余明细待审");
        } else if (hasRejected) {
            // 无 pending 但有 rejected：整体驳回
            upd.setStatus("rejected");
            upd.setAuditBy(userId);
            upd.setAuditTime(now);
            upd.setAuditRemark("明细全部处理完毕，存在驳回项（审批单：" + approval.getApprovalCode() + "）");
        } else {
            // 无 pending、无 rejected：全部 approved/established → 整体 approved
            upd.setStatus("approved");
            upd.setAuditBy(userId);
            upd.setAuditTime(now);
            upd.setAuditRemark("明细全部立项通过（审批单：" + approval.getApprovalCode() + "）");
        }
        declarationMapper.updateById(upd);
        return detailCount;
    }

    private ProjectApprovalDetail buildHouseholdDetail(Long declareId, DeclarationHousehold hh) {
        ProjectApprovalDetail pd = new ProjectApprovalDetail();
        pd.setDeclareId(declareId);
        pd.setDetailType(Constants.APPROVAL_DETAIL_TYPE_HOUSEHOLD);
        pd.setHouseholdId(hh.getId());
        pd.setTargetId(hh.getHouseholdId());
        pd.setTargetName(hh.getSnapshotHeadName());
        // 农户明细编号使用户号（业务编码），而非户主身份证号
        pd.setTargetCode(hh.getSnapshotHouseholdNo());
        pd.setRegionName(hh.getSnapshotRegionName());
        pd.setAmount(hh.getAmount() != null ? hh.getAmount() : BigDecimal.ZERO);
        pd.setCreateTime(LocalDateTime.now());
        return pd;
    }

    private ProjectApprovalDetail buildFacilityDetail(Long declareId, DeclarationPublicFacility pf) {
        ProjectApprovalDetail pd = new ProjectApprovalDetail();
        pd.setDeclareId(declareId);
        pd.setDetailType(Constants.APPROVAL_DETAIL_TYPE_FACILITY);
        pd.setFacilityId(pf.getId());
        pd.setTargetId(pf.getFacilityId());
        pd.setTargetName(pf.getSnapshotFacilityName());
        pd.setTargetCode(pf.getSnapshotFacilityCode());
        pd.setRegionName(pf.getSnapshotRegionName());
        pd.setAmount(pf.getAmount() != null ? pf.getAmount() : BigDecimal.ZERO);
        pd.setCreateTime(LocalDateTime.now());
        return pd;
    }

    /** 是否命中本次立项选中的明细；未指定明细时默认全选（兼容手动输入申报单 ID） */
    private boolean isDetailPicked(Set<Long> selectedIds, Long detailId) {
        return selectedIds.isEmpty() || selectedIds.contains(detailId);
    }

    /** 明细审核状态中文描述，用于立项冲突提示 */
    private String auditStatusLabel(String status) {
        if (status == null || status.isBlank() || "pending".equals(status)) {
            return "待审核";
        }
        return switch (status) {
            case "approved" -> "已通过";
            case "rejected" -> "已驳回";
            case "established" -> "已立项";
            case "unsubmitted" -> "未提交";
            default -> status;
        };
    }

    private String currentUser() {
        LoginUser loginUser = UserContext.get();
        if (loginUser != null && loginUser.getUsername() != null && !loginUser.getUsername().isBlank()) {
            return loginUser.getUsername();
        }
        return "system";
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return isNotBlank(value) ? value.trim() : null;
    }
}

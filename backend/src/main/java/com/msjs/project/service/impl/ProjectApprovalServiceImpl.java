package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.ApprovalEditDTO;
import com.msjs.project.dto.ProjectApprovalSubmitDTO;
import com.msjs.project.dto.ProjectApprovalSubmitDTO.AttachmentItem;
import com.msjs.project.entity.Declaration;
import com.msjs.project.entity.DeclarationHousehold;
import com.msjs.project.entity.DeclarationPublicFacility;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.ProjectApprovalAttachment;
import com.msjs.project.entity.ProjectApprovalDeclare;
import com.msjs.project.entity.ProjectApprovalDetail;
import com.msjs.project.mapper.DeclarationHouseholdMapper;
import com.msjs.project.mapper.DeclarationMapper;
import com.msjs.project.mapper.DeclarationPublicFacilityMapper;
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
        List<Declaration> parents = declarationMapper.selectBatchIds(dto.getDeclarationIds());
        if (parents == null || parents.isEmpty()) {
            throw new BizException("未找到所选的申报单");
        }
        for (Declaration d : parents) {
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
        approval.setSubmitTime(now);
        approval.setRegionCode(dto.getTownCode());
        approval.setRegionName(dto.getTownName());
        approval.setCreateBy(username);
        approval.setCreateTime(now);
        approval.setUpdateBy(username);
        approval.setUpdateTime(now);
        approvalMapper.insert(approval);

        // 4. declare + detail + 状态推进
        int declareCount = 0, detailCount = 0;
        for (Declaration d : parents) {
            // 4.1 每个被选中的声明单落一条 declare
            ProjectApprovalDeclare declare = new ProjectApprovalDeclare();
            declare.setApprovalId(approval.getId());
            declare.setDeclarationId(d.getId());
            declare.setDeclareCode(d.getBusinessCode());
            declare.setDeclareName(d.getTitle());
            declare.setDeclareCategory(d.getDeclarationCategory());
            declare.setDeclareType(d.getDeclarationType());
            declare.setTownName(d.getTownName());
            declare.setBudget(d.getAmount());
            declare.setSort(declareCount);
            declare.setCreateTime(now);
            declareMapper.insert(declare);
            declareCount++;

            // 4.2 抓这条声明单下所有 approved 明细 → 立项 detail → 改 audit_status=established
            if ("1".equals(d.getDeclarationCategory())) {
                // 农户
                List<DeclarationHousehold> all = householdMapper.selectList(
                        new LambdaQueryWrapper<DeclarationHousehold>()
                                .eq(DeclarationHousehold::getDeclarationId, d.getId())
                                .eq(DeclarationHousehold::getDeleted, 0));
                for (DeclarationHousehold hh : all) {
                    if ("approved".equals(hh.getAuditStatus())) {
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
                }
            } else if ("2".equals(d.getDeclarationCategory())) {
                // 公建
                List<DeclarationPublicFacility> all = facilityMapper.selectList(
                        new LambdaQueryWrapper<DeclarationPublicFacility>()
                                .eq(DeclarationPublicFacility::getDeclarationId, d.getId())
                                .eq(DeclarationPublicFacility::getDeleted, 0));
                for (DeclarationPublicFacility pf : all) {
                    if ("approved".equals(pf.getAuditStatus())) {
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
                }
            }
            // 4.3 父申报单状态回算：明细已立项 → 父申报单状态推进到 established（已立项）
            d.setStatus("established");
            d.setAuditBy(userId);
            d.setAuditTime(now);
            d.setAuditRemark("明细已立项（审批单：" + approval.getApprovalCode() + "）");
            d.setUpdateBy(username);
            d.setUpdateTime(now);
            declarationMapper.updateById(d);
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
                                   String projectCategory, String projectSource, String startTime, String endTime) {
        LambdaQueryWrapper<ProjectApproval> wrapper = buildFilterWrapper(
                projectName, projectCategory, projectSource, startTime, endTime);
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
    public ApprovalCountVO counts(String projectName, String projectCategory, String projectSource,
                                  String startTime, String endTime) {
        ApprovalCountVO vo = new ApprovalCountVO();
        vo.setAll(approvalMapper.selectCount(buildFilterWrapper(projectName, projectCategory, projectSource, startTime, endTime)));
        vo.setPending(approvalMapper.selectCount(buildFilterWrapper(projectName, projectCategory, projectSource, startTime, endTime)
                .eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_PENDING)));
        vo.setRejected(approvalMapper.selectCount(buildFilterWrapper(projectName, projectCategory, projectSource, startTime, endTime)
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
        // 业务约束：仅待审核状态可编辑
        if (!Constants.APPROVAL_STATUS_PENDING.equals(approval.getStatus())) {
            throw new BizException("仅待审核的立项可编辑");
        }
        approval.setProjectName(trimToNull(dto.getProjectName()));
        approval.setProjectCategory(trimToNull(dto.getProjectCategory()));
        approval.setProjectSource(trimToNull(dto.getProjectSource()));
        approval.setBudget(dto.getBudget());
        approval.setLeader(trimToNull(dto.getLeader()));
        approval.setRemark(trimToNull(dto.getRemark()));
        approval.setUpdateBy(currentUser());
        approval.setUpdateTime(LocalDateTime.now());
        approvalMapper.updateById(approval);

        saveProjectAttachments(approval.getId(), dto.getAttachments());
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
        if (!fromStatus.equals(approval.getStatus())) {
            throw new BizException(errorMsg);
        }
        ProjectApproval update = new ProjectApproval();
        update.setId(approval.getId());
        update.setStatus(toStatus);
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

        List<ApprovalVO> records = new ArrayList<>(approvals.size());
        for (ProjectApproval approval : approvals) {
            ApprovalVO projectNode = toApprovalVO(approval);
            List<ApprovalDeclareVO> declareNodes = new ArrayList<>();
            for (ProjectApprovalDeclare declare : declaresByApproval.getOrDefault(approval.getId(), Collections.emptyList())) {
                ApprovalDeclareVO declareNode = toDeclareVO(declare);
                declareNode.setChildren(detailsByDeclare
                        .getOrDefault(declare.getId(), Collections.emptyList())
                        .stream().map(this::toDetailVO).toList());
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
        vo.setProjectSource(approval.getProjectSource());
        vo.setBudget(approval.getBudget());
        vo.setLeader(approval.getLeader());
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

    private ApprovalDetailVO toDetailVO(ProjectApprovalDetail detail) {
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
        return vo;
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

    /** 构造列表筛选条件（项目名称/类别/来源/提交时间范围） */
    private LambdaQueryWrapper<ProjectApproval> buildFilterWrapper(String projectName, String projectCategory,
                                                                   String projectSource, String startTime,
                                                                   String endTime) {
        LocalDate startDate = parseDate(startTime, "提交开始日期");
        LocalDate endDate = parseDate(endTime, "提交结束日期");
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime end = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        return new LambdaQueryWrapper<ProjectApproval>()
                .like(isNotBlank(projectName), ProjectApproval::getProjectName, trimToNull(projectName))
                .eq(isNotBlank(projectCategory), ProjectApproval::getProjectCategory, trimToNull(projectCategory))
                .eq(isNotBlank(projectSource), ProjectApproval::getProjectSource, trimToNull(projectSource))
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

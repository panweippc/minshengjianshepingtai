package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.ConstructionEditDTO;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.ProjectApprovalDeclare;
import com.msjs.project.entity.ProjectApprovalDetail;
import com.msjs.project.entity.ProjectConstruction;
import com.msjs.project.entity.ProjectConstructionAttachment;
import com.msjs.project.entity.ProjectConstructionLog;
import com.msjs.project.mapper.ProjectApprovalDeclareMapper;
import com.msjs.project.mapper.ProjectApprovalDetailMapper;
import com.msjs.project.mapper.ProjectApprovalMapper;
import com.msjs.project.mapper.ProjectConstructionAttachmentMapper;
import com.msjs.project.mapper.ProjectConstructionLogMapper;
import com.msjs.project.mapper.ProjectConstructionMapper;
import com.msjs.project.service.ProjectConstructionService;
import com.msjs.project.vo.ConstructionAttachmentVO;
import com.msjs.project.vo.ConstructionLogVO;
import com.msjs.project.vo.ConstructionNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 施工管理服务实现
 * 树形结构：项目（一级） → 申报内容（二级） → 农户/设施明细（三级）
 * 施工状态仅记录在三级明细，一级/二级状态由三级明细实时聚合：
 * 有任意一户完工即展示“已完工”（分子为已完工数），否则有开工户即展示“施工中”（分子为已开工数）
 */
@Service
@RequiredArgsConstructor
public class ProjectConstructionServiceImpl implements ProjectConstructionService {

    private final ProjectApprovalMapper approvalMapper;
    private final ProjectApprovalDeclareMapper declareMapper;
    private final ProjectApprovalDetailMapper detailMapper;
    private final ProjectConstructionMapper constructionMapper;
    private final ProjectConstructionLogMapper constructionLogMapper;
    private final ProjectConstructionAttachmentMapper constructionAttachmentMapper;

    @Override
    public PageVO<ConstructionNodeVO> page(long pageNum, long pageSize, String projectCode, String projectName,
                                           String projectCategory, String projectType) {
        Page<ProjectApproval> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProjectApproval> wrapper = new LambdaQueryWrapper<>();
        // 业务约束：只有审核通过的立项才进入施工阶段
        wrapper.eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_APPROVED)
                .like(isNotBlank(projectCode), ProjectApproval::getApprovalCode, trimToNull(projectCode))
                .like(isNotBlank(projectName), ProjectApproval::getProjectName, trimToNull(projectName))
                .eq(isNotBlank(projectCategory), ProjectApproval::getProjectCategory, projectCategory)
                // 项目类型与立项共用 project_source 字段存储，筛选字典为 xiang_mu_lei_xing
                .eq(isNotBlank(projectType), ProjectApproval::getProjectSource, projectType)
                .orderByDesc(ProjectApproval::getSubmitTime)
                .orderByDesc(ProjectApproval::getId);
        Page<ProjectApproval> result = approvalMapper.selectPage(page, wrapper);

        PageVO<ConstructionNodeVO> vo = new PageVO<>();
        vo.setRecords(buildTree(result.getRecords()));
        vo.setTotal(result.getTotal());
        vo.setPageNum(result.getCurrent());
        vo.setPageSize(result.getSize());
        return vo;
    }

    @Override
    public ConstructionNodeVO detail(Long approvalId) {
        ProjectApproval approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BizException("立项信息不存在或已被删除");
        }
        if (!Constants.APPROVAL_STATUS_APPROVED.equals(approval.getStatus())) {
            throw new BizException("仅审核通过的立项可查看施工信息");
        }
        ConstructionNodeVO node = buildTree(List.of(approval)).get(0);

        List<ProjectConstructionLog> logs = constructionLogMapper.selectList(
                new LambdaQueryWrapper<ProjectConstructionLog>()
                        .eq(ProjectConstructionLog::getApprovalId, approvalId)
                        .orderByDesc(ProjectConstructionLog::getConstructionDate)
                        .orderByDesc(ProjectConstructionLog::getId));
        node.setLogs(logs.stream().map(this::toLogVO).toList());
        node.setLogCount(logs.size());
        node.setInspectionCount(logs.size());
        long passCount = logs.stream()
                .filter(item -> Constants.CONSTRUCTION_INSPECTION_PASS.equals(item.getInspectionStatus()))
                .count();
        node.setInspectionPassCount((int) passCount);
        node.setInspectionPassRate(logs.isEmpty() ? 0
                : (int) Math.round(passCount * 100.0 / logs.size()));

        List<ProjectConstructionAttachment> attachments = constructionAttachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectConstructionAttachment>()
                        .eq(ProjectConstructionAttachment::getApprovalId, approvalId)
                        .orderByAsc(ProjectConstructionAttachment::getId));
        node.setAttachments(attachments.stream().map(this::toAttachmentVO).toList());
        return node;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(ConstructionEditDTO dto) {
        ProjectApproval approval = approvalMapper.selectById(dto.getApprovalId());
        if (approval == null) {
            throw new BizException("立项信息不存在或已被删除");
        }
        // 业务约束：仅审核通过的立项可维护施工信息
        if (!Constants.APPROVAL_STATUS_APPROVED.equals(approval.getStatus())) {
            throw new BizException("仅审核通过的立项可维护施工信息");
        }
        validateDates(dto.getStartDate(), dto.getCompleteDate());

        if (Constants.CONSTRUCTION_LEVEL_PROJECT == dto.getNodeLevel()) {
            saveProjectNode(approval.getId(), dto);
        } else if (Constants.CONSTRUCTION_LEVEL_DETAIL == dto.getNodeLevel()) {
            saveDetailNode(approval.getId(), dto);
        } else {
            throw new BizException("不支持的施工节点层级");
        }
    }

    /** 保存一级（项目）施工信息：施工单位、工期、备注 */
    private void saveProjectNode(Long approvalId, ConstructionEditDTO dto) {
        ProjectConstruction exist = constructionMapper.selectOne(new LambdaQueryWrapper<ProjectConstruction>()
                .eq(ProjectConstruction::getApprovalId, approvalId)
                .eq(ProjectConstruction::getNodeLevel, Constants.CONSTRUCTION_LEVEL_PROJECT)
                .last("LIMIT 1"));
        ProjectConstruction entity = exist == null ? new ProjectConstruction() : exist;
        entity.setApprovalId(approvalId);
        entity.setNodeLevel(Constants.CONSTRUCTION_LEVEL_PROJECT);
        entity.setBuilder(trimToNull(dto.getBuilder()));
        entity.setContractorName(trimToNull(dto.getContractorName()));
        entity.setContractorPhone(trimToNull(dto.getContractorPhone()));
        entity.setLeaderName(trimToNull(dto.getLeaderName()));
        entity.setLeaderPhone(trimToNull(dto.getLeaderPhone()));
        // 新编辑界面仅维护施工队信息与附件，工期/备注非空时才更新，避免误清空
        if (dto.getStartDate() != null) {
            entity.setStartDate(dto.getStartDate());
        }
        if (dto.getCompleteDate() != null) {
            entity.setCompleteDate(dto.getCompleteDate());
        }
        if (dto.getRemark() != null) {
            entity.setRemark(trimToNull(dto.getRemark()));
        }
        saveEntity(entity, exist == null);
        saveAttachments(approvalId, dto.getAttachments());
    }

    /** 保存三级（农户/设施明细）施工状态与工期 */
    private void saveDetailNode(Long approvalId, ConstructionEditDTO dto) {
        if (dto.getDetailId() == null) {
            throw new BizException("明细ID不能为空");
        }
        ProjectApprovalDetail detail = detailMapper.selectById(dto.getDetailId());
        if (detail == null) {
            throw new BizException("施工明细不存在或已被删除");
        }
        ProjectApprovalDeclare declare = declareMapper.selectById(detail.getDeclareId());
        if (declare == null || !approvalId.equals(declare.getApprovalId())) {
            throw new BizException("施工明细不属于该立项");
        }
        String buildStatus = trimToNull(dto.getBuildStatus());
        Set<String> allowed = Set.of(Constants.CONSTRUCTION_STATUS_NOT_STARTED,
                Constants.CONSTRUCTION_STATUS_IN_PROGRESS, Constants.CONSTRUCTION_STATUS_COMPLETED);
        if (buildStatus == null || !allowed.contains(buildStatus)) {
            throw new BizException("施工状态不合法");
        }

        ProjectConstruction exist = constructionMapper.selectOne(new LambdaQueryWrapper<ProjectConstruction>()
                .eq(ProjectConstruction::getDetailId, detail.getId())
                .last("LIMIT 1"));
        ProjectConstruction entity = exist == null ? new ProjectConstruction() : exist;
        entity.setApprovalId(approvalId);
        entity.setDeclareId(detail.getDeclareId());
        entity.setDetailId(detail.getId());
        entity.setNodeLevel(Constants.CONSTRUCTION_LEVEL_DETAIL);
        entity.setBuildStatus(buildStatus);
        entity.setStartDate(dto.getStartDate());
        entity.setCompleteDate(dto.getCompleteDate());
        entity.setRemark(trimToNull(dto.getRemark()));
        saveEntity(entity, exist == null);
    }

    private void saveEntity(ProjectConstruction entity, boolean create) {
        String user = currentUser();
        entity.setUpdateBy(user);
        entity.setUpdateTime(LocalDateTime.now());
        if (create) {
            entity.setCreateBy(user);
            entity.setCreateTime(LocalDateTime.now());
            entity.setDeleted(0);
            constructionMapper.insert(entity);
        } else {
            constructionMapper.updateById(entity);
        }
    }

    private void validateDates(LocalDate startDate, LocalDate completeDate) {
        if (startDate != null && completeDate != null && completeDate.isBefore(startDate)) {
            throw new BizException("完工日期不能早于开工日期");
        }
    }

    /**
     * 批量组装三级树并聚合施工进度，避免 N+1 查询
     */
    private List<ConstructionNodeVO> buildTree(List<ProjectApproval> approvals) {
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

        // 施工记录：一级按立项ID取，三级按明细ID取
        List<ProjectConstruction> constructions = constructionMapper.selectList(
                new LambdaQueryWrapper<ProjectConstruction>()
                        .in(ProjectConstruction::getApprovalId, approvalIds));
        Map<Long, ProjectConstruction> projectMap = constructions.stream()
                .filter(item -> Constants.CONSTRUCTION_LEVEL_PROJECT == item.getNodeLevel())
                .collect(Collectors.toMap(ProjectConstruction::getApprovalId, Function.identity(), (a, b) -> a));
        Map<Long, ProjectConstruction> detailMap = constructions.stream()
                .filter(item -> Constants.CONSTRUCTION_LEVEL_DETAIL == item.getNodeLevel() && item.getDetailId() != null)
                .collect(Collectors.toMap(ProjectConstruction::getDetailId, Function.identity(), (a, b) -> a));

        Map<Long, List<ProjectApprovalDetail>> detailsByDeclare = details.stream()
                .collect(Collectors.groupingBy(ProjectApprovalDetail::getDeclareId));
        Map<Long, List<ProjectApprovalDeclare>> declaresByApproval = declares.stream()
                .collect(Collectors.groupingBy(ProjectApprovalDeclare::getApprovalId));

        List<ConstructionNodeVO> records = new ArrayList<>(approvals.size());
        for (ProjectApproval approval : approvals) {
            ConstructionNodeVO projectNode = buildProjectNode(approval, projectMap.get(approval.getId()));
            List<ConstructionNodeVO> declareNodes = new ArrayList<>();
            for (ProjectApprovalDeclare declare : declaresByApproval.getOrDefault(approval.getId(), Collections.emptyList())) {
                ConstructionNodeVO declareNode = buildDeclareNode(approval.getId(), declare);
                List<ConstructionNodeVO> detailNodes = new ArrayList<>();
                for (ProjectApprovalDetail detail : detailsByDeclare.getOrDefault(declare.getId(), Collections.emptyList())) {
                    detailNodes.add(buildDetailNode(approval.getId(), detail, detailMap.get(detail.getId())));
                }
                declareNode.setChildren(detailNodes);
                aggregate(declareNode, detailNodes);
                declareNodes.add(declareNode);
            }
            projectNode.setChildren(declareNodes);
            // 项目进度按全部申报明细聚合
            List<ConstructionNodeVO> allDetails = declareNodes.stream()
                    .flatMap(item -> item.getChildren().stream())
                    .toList();
            aggregate(projectNode, allDetails);
            // 验收按钮仅在项目层级、且全部明细完工时展示
            projectNode.setCanAccept(Boolean.TRUE.equals(projectNode.getCanAccept()));
            records.add(projectNode);
        }
        return records;
    }

    private ConstructionNodeVO buildProjectNode(ProjectApproval approval, ProjectConstruction construction) {
        ConstructionNodeVO node = new ConstructionNodeVO();
        node.setNodeKey("p_" + approval.getId());
        node.setNodeType("project");
        node.setApprovalId(approval.getId());
        node.setCode(approval.getApprovalCode());
        node.setName(approval.getProjectName());
        node.setCategory(approval.getProjectCategory());
        node.setSource(approval.getProjectSource());
        if (construction != null) {
            node.setId(construction.getId());
            node.setBuilder(construction.getBuilder());
            node.setContractorName(construction.getContractorName());
            node.setContractorPhone(construction.getContractorPhone());
            node.setLeaderName(construction.getLeaderName());
            node.setLeaderPhone(construction.getLeaderPhone());
            node.setStartDate(construction.getStartDate());
            node.setCompleteDate(construction.getCompleteDate());
            node.setRemark(construction.getRemark());
        }
        return node;
    }

    private ConstructionNodeVO buildDeclareNode(Long approvalId, ProjectApprovalDeclare declare) {
        ConstructionNodeVO node = new ConstructionNodeVO();
        node.setNodeKey("d_" + declare.getId());
        node.setNodeType("declare");
        node.setApprovalId(approvalId);
        node.setDeclareId(declare.getId());
        node.setCode(declare.getDeclareCode());
        node.setName(declare.getDeclareName());
        node.setCategory(declare.getDeclareCategory());
        node.setSource(declare.getDeclareType());
        return node;
    }

    private ConstructionNodeVO buildDetailNode(Long approvalId, ProjectApprovalDetail detail,
                                               ProjectConstruction construction) {
        ConstructionNodeVO node = new ConstructionNodeVO();
        node.setNodeKey("t_" + detail.getId());
        node.setNodeType("detail");
        node.setApprovalId(approvalId);
        node.setDeclareId(detail.getDeclareId());
        node.setDetailId(detail.getId());
        node.setCode(detail.getTargetCode());
        node.setName(detail.getTargetName());
        node.setCategory(detail.getDetailType());
        node.setDetailType(detail.getDetailType());
        node.setSource(detail.getRegionName());
        node.setAmount(detail.getAmount());
        // 备注优先展示施工记录备注（编辑弹窗回填），无施工记录时展示立项明细事项说明
        node.setRemark(construction != null ? construction.getRemark() : detail.getRemark());
        String buildStatus = construction == null ? null : trimToNull(construction.getBuildStatus());
        if (buildStatus == null) {
            buildStatus = Constants.CONSTRUCTION_STATUS_NOT_STARTED;
        }
        node.setBuildStatus(buildStatus);
        if (construction != null) {
            node.setId(construction.getId());
            node.setStartDate(construction.getStartDate());
            node.setCompleteDate(construction.getCompleteDate());
        }
        // 三级节点为单个农户/设施：分母固定为1
        int numerator = Constants.CONSTRUCTION_STATUS_NOT_STARTED.equals(buildStatus) ? 0 : 1;
        node.setStatus(buildStatus);
        node.setTotalCount(1);
        node.setCompletedCount(Constants.CONSTRUCTION_STATUS_COMPLETED.equals(buildStatus) ? 1 : 0);
        node.setStartedCount(Constants.CONSTRUCTION_STATUS_IN_PROGRESS.equals(buildStatus) ? 1 : 0);
        node.setNumerator(numerator);
        node.setPercent(numerator * 100);
        node.setCanAccept(false);
        return node;
    }

    /**
     * 按三级明细聚合一级/二级施工状态：
     * 有完工 → 已完工（分子=完工数）；否则有开工 → 施工中（分子=已开工数）；否则未开工
     */
    private void aggregate(ConstructionNodeVO node, List<ConstructionNodeVO> details) {
        int total = details.size();
        int started = 0;
        int completed = 0;
        BigDecimal amount = BigDecimal.ZERO;
        for (ConstructionNodeVO detail : details) {
            String status = detail.getBuildStatus();
            if (Constants.CONSTRUCTION_STATUS_COMPLETED.equals(status)) {
                completed++;
            } else if (Constants.CONSTRUCTION_STATUS_IN_PROGRESS.equals(status)) {
                started++;
            }
            if (detail.getAmount() != null) {
                amount = amount.add(detail.getAmount());
            }
        }
        String status;
        int numerator;
        if (completed > 0) {
            status = Constants.CONSTRUCTION_STATUS_COMPLETED;
            numerator = completed;
        } else if (started > 0) {
            status = Constants.CONSTRUCTION_STATUS_IN_PROGRESS;
            numerator = started;
        } else {
            status = Constants.CONSTRUCTION_STATUS_NOT_STARTED;
            numerator = 0;
        }
        node.setTotalCount(total);
        node.setStartedCount(started);
        node.setCompletedCount(completed);
        node.setAmount(amount);
        node.setStatus(status);
        node.setNumerator(numerator);
        node.setPercent(total > 0 ? (int) Math.round(numerator * 100.0 / total) : 0);
        node.setCanAccept(total > 0 && completed == total);
    }

    /**
     * 保存施工资料附件：
     * 已存在附件按ID差异更新，本次未提交的已有附件逻辑删除，新增附件直接插入
     */
    private void saveAttachments(Long approvalId, List<ConstructionEditDTO.AttachmentItem> attachments) {
        if (attachments == null) {
            // 未传附件列表视为不修改附件
            return;
        }
        List<ProjectConstructionAttachment> existList = constructionAttachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectConstructionAttachment>()
                        .eq(ProjectConstructionAttachment::getApprovalId, approvalId));
        Set<Long> inputIds = new HashSet<>();
        for (ConstructionEditDTO.AttachmentItem item : attachments) {
            if (item.getId() != null) {
                inputIds.add(item.getId());
            }
        }
        for (ProjectConstructionAttachment att : existList) {
            if (!inputIds.contains(att.getId())) {
                constructionAttachmentMapper.deleteById(att.getId());
            }
        }
        for (ConstructionEditDTO.AttachmentItem item : attachments) {
            if (item.getId() != null) {
                ProjectConstructionAttachment att = constructionAttachmentMapper.selectById(item.getId());
                if (att == null || !approvalId.equals(att.getApprovalId())) {
                    throw new BizException("施工资料附件不存在或不属于该项目");
                }
                fillAttachment(att, item);
                constructionAttachmentMapper.updateById(att);
                continue;
            }
            ProjectConstructionAttachment att = new ProjectConstructionAttachment();
            att.setApprovalId(approvalId);
            fillAttachment(att, item);
            att.setCreateBy(currentUser());
            att.setCreateTime(LocalDateTime.now());
            att.setDeleted(0);
            constructionAttachmentMapper.insert(att);
        }
    }

    private void fillAttachment(ProjectConstructionAttachment att, ConstructionEditDTO.AttachmentItem item) {
        att.setAttachmentType(trimToNull(item.getAttachmentType()));
        att.setFileName(trimToNull(item.getFileName()));
        att.setFileKey(trimToNull(item.getFileKey()));
        att.setBucket(trimToNull(item.getBucket()));
        att.setFileSize(item.getFileSize());
        att.setRemark(trimToNull(item.getRemark()));
    }

    private ConstructionLogVO toLogVO(ProjectConstructionLog log) {
        ConstructionLogVO vo = new ConstructionLogVO();
        vo.setId(log.getId());
        vo.setLogCode(log.getLogCode());
        vo.setConstructionDate(log.getConstructionDate());
        vo.setInspector(log.getInspector());
        vo.setInspectionStatus(log.getInspectionStatus());
        vo.setConstructionStatus(log.getConstructionStatus());
        vo.setContent(log.getContent());
        return vo;
    }

    private ConstructionAttachmentVO toAttachmentVO(ProjectConstructionAttachment attachment) {
        ConstructionAttachmentVO vo = new ConstructionAttachmentVO();
        vo.setId(attachment.getId());
        vo.setAttachmentType(attachment.getAttachmentType());
        vo.setFileName(attachment.getFileName());
        vo.setFileKey(attachment.getFileKey());
        vo.setBucket(attachment.getBucket());
        vo.setFileSize(attachment.getFileSize());
        vo.setRemark(attachment.getRemark());
        return vo;
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

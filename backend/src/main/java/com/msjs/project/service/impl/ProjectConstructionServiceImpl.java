package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.ConstructionEditDTO;
import com.msjs.project.dto.ConstructionLogEditDTO;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        // 业务约束：只有审核通过、尚未发起验收的立项才进入施工阶段
        wrapper.eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_APPROVED)
                .eq(ProjectApproval::getAcceptStatus, Constants.APPROVAL_ACCEPT_NONE)
                .like(isNotBlank(projectCode), ProjectApproval::getApprovalCode, trimToNull(projectCode))
                .like(isNotBlank(projectName), ProjectApproval::getProjectName, trimToNull(projectName))
                .eq(isNotBlank(projectCategory), ProjectApproval::getProjectCategory, projectCategory)
                // 项目类型筛选（字典 xiang_mu_lei_xing，对应 project_type 列）
                .eq(isNotBlank(projectType), ProjectApproval::getProjectType, projectType)
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

        // 二级查看明细进度：为每个三级明细构建 detailLogSnapshot
        // 与三级查看详情页签完全同源（同样的取最新日志排序 + 无日志回退 buildStatus + 附件按 log_id 留痕）
        List<Long> allDetailIds = node.getChildren() == null ? List.of() : node.getChildren().stream()
                .filter(c -> c.getChildren() != null)
                .flatMap(c -> c.getChildren().stream())
                .map(ConstructionNodeVO::getDetailId)
                .filter(java.util.Objects::nonNull)
                .distinct().toList();
        // 1) 从已查日志（constructionDate DESC, id DESC）中按 detailId 取最新一条
        Map<Long, ProjectConstructionLog> latestLogByDetailId = new HashMap<>();
        for (ProjectConstructionLog log : logs) {
            if (log.getDetailId() != null && !latestLogByDetailId.containsKey(log.getDetailId())) {
                latestLogByDetailId.put(log.getDetailId(), log);
            }
        }
        // 2) 无日志的 detailId：批量查 ProjectConstruction 回退 buildStatus
        List<Long> noLogDetailIds = allDetailIds.stream()
                .filter(dId -> !latestLogByDetailId.containsKey(dId)).toList();
        Map<Long, ProjectConstruction> constructionByDetailId = new HashMap<>();
        if (!noLogDetailIds.isEmpty()) {
            constructionByDetailId = constructionMapper.selectList(
                    new LambdaQueryWrapper<ProjectConstruction>()
                            .in(ProjectConstruction::getDetailId, noLogDetailIds))
                    .stream().collect(Collectors.toMap(ProjectConstruction::getDetailId, c -> c, (a, b) -> a));
        }
        // 3) 批量查最新日志的附件（按 log_id，id ASC）
        Map<Long, List<ProjectConstructionAttachment>> attachmentsByLogId = new HashMap<>();
        List<Long> latestLogIds = latestLogByDetailId.values().stream().map(ProjectConstructionLog::getId).distinct().toList();
        if (!latestLogIds.isEmpty()) {
            List<ProjectConstructionAttachment> allAtts = constructionAttachmentMapper.selectList(
                    new LambdaQueryWrapper<ProjectConstructionAttachment>()
                            .in(ProjectConstructionAttachment::getLogId, latestLogIds)
                            .orderByAsc(ProjectConstructionAttachment::getId));
            attachmentsByLogId = allAtts.stream()
                    .collect(Collectors.groupingBy(ProjectConstructionAttachment::getLogId));
        }
        // 4) 组装 detailLogSnapshot 回填到每个 detailNode
        if (node.getChildren() != null) {
            for (ConstructionNodeVO declareNode : node.getChildren()) {
                if (declareNode.getChildren() == null) {
                    continue;
                }
                for (ConstructionNodeVO detailNode : declareNode.getChildren()) {
                    Long dId = detailNode.getDetailId();
                    if (dId == null) {
                        continue;
                    }
                    ConstructionLogVO snapshot;
                    ProjectConstructionLog latestLog = latestLogByDetailId.get(dId);
                    if (latestLog != null) {
                        snapshot = toLogVO(latestLog);
                        List<ConstructionAttachmentVO> atts = attachmentsByLogId
                                .getOrDefault(latestLog.getId(), Collections.emptyList())
                                .stream().map(this::toAttachmentVO).toList();
                        snapshot.setAttachments(atts);
                    } else {
                        // 尚未录入施工记录：回退 buildStatus，其余字段留空
                        snapshot = new ConstructionLogVO();
                        ProjectConstruction construction = constructionByDetailId.get(dId);
                        String buildStatus = construction == null ? null : trimToNull(construction.getBuildStatus());
                        snapshot.setConstructionStatus(buildStatus == null
                                ? Constants.CONSTRUCTION_STATUS_NOT_STARTED : buildStatus);
                        snapshot.setAttachments(Collections.emptyList());
                    }
                    snapshot.setDetailId(dId);
                    detailNode.setDetailLogSnapshot(snapshot);
                }
            }
        }
        node.setInspectionCount(logs.size());
        long passCount = logs.stream()
                .filter(item -> Constants.CONSTRUCTION_INSPECTION_PASS.equals(item.getInspectionStatus()))
                .count();
        node.setInspectionPassCount((int) passCount);
        node.setInspectionPassRate(logs.isEmpty() ? 0
                : (int) Math.round(passCount * 100.0 / logs.size()));

        // 项目级施工资料附件：仅 detailId 为空、logId 为空的通用附件
        List<ProjectConstructionAttachment> attachments = constructionAttachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectConstructionAttachment>()
                        .eq(ProjectConstructionAttachment::getApprovalId, approvalId)
                        .isNull(ProjectConstructionAttachment::getDetailId)
                        .isNull(ProjectConstructionAttachment::getLogId)
                        .orderByAsc(ProjectConstructionAttachment::getId));
        node.setAttachments(attachments.stream().map(this::toAttachmentVO).toList());
        return node;
    }

    @Override
    public ConstructionLogVO detailLog(Long detailId) {
        ProjectApprovalDetail detail = detailMapper.selectById(detailId);
        if (detail == null) {
            throw new BizException("施工明细不存在或已被删除");
        }
        ProjectApprovalDeclare declare = declareMapper.selectById(detail.getDeclareId());
        if (declare == null) {
            throw new BizException("施工明细所属申报不存在");
        }
        ProjectApproval approval = approvalMapper.selectById(declare.getApprovalId());
        if (approval == null || !Constants.APPROVAL_STATUS_APPROVED.equals(approval.getStatus())) {
            throw new BizException("仅审核通过的立项可查看施工记录");
        }

        // 与二级查看明细进度同源：最新施工记录快照
        ConstructionLogVO vo = buildLatestLogSnapshot(detailId);
        vo.setProjectName(approval.getProjectName());
        vo.setDetailName(detail.getTargetName());
        return vo;
    }

    /**
     * 最新施工记录快照：与三级查看详情页签完全同源
     * <ol>
     *   <li>按 detailId 取最新一条日志（constructionDate DESC, id DESC）</li>
     *   <li>无日志时回退到 ProjectConstruction.buildStatus，其余字段留空</li>
     *   <li>附件取该日志按 log_id 绑定的留痕快照（id ASC）</li>
     * </ol>
     */
    private ConstructionLogVO buildLatestLogSnapshot(Long detailId) {
        ProjectConstructionLog log = constructionLogMapper.selectOne(
                new LambdaQueryWrapper<ProjectConstructionLog>()
                        .eq(ProjectConstructionLog::getDetailId, detailId)
                        .orderByDesc(ProjectConstructionLog::getConstructionDate)
                        .orderByDesc(ProjectConstructionLog::getId)
                        .last("LIMIT 1"));
        ConstructionLogVO vo;
        if (log != null) {
            vo = toLogVO(log);
        } else {
            // 尚未录入施工记录的明细：回显当前施工状态，其余字段留空
            vo = new ConstructionLogVO();
            ProjectConstruction construction = constructionMapper.selectOne(
                    new LambdaQueryWrapper<ProjectConstruction>()
                            .eq(ProjectConstruction::getDetailId, detailId)
                            .last("LIMIT 1"));
            String buildStatus = construction == null ? null : trimToNull(construction.getBuildStatus());
            vo.setConstructionStatus(buildStatus == null
                    ? Constants.CONSTRUCTION_STATUS_NOT_STARTED : buildStatus);
        }
        vo.setDetailId(detailId);

        // 附件取最新一条施工记录留痕的附件（无日志时为空）
        List<ProjectConstructionAttachment> attachments = log == null
                ? List.of()
                : constructionAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectConstructionAttachment>()
                                .eq(ProjectConstructionAttachment::getLogId, log.getId())
                                .orderByAsc(ProjectConstructionAttachment::getId));
        vo.setAttachments(attachments.stream().map(this::toAttachmentVO).toList());
        return vo;
    }

    @Override
    public List<ConstructionLogVO> detailLogHistory(Long detailId) {
        ProjectApprovalDetail detail = detailMapper.selectById(detailId);
        if (detail == null) {
            throw new BizException("施工明细不存在或已被删除");
        }
        List<ProjectConstructionLog> logs = constructionLogMapper.selectList(
                new LambdaQueryWrapper<ProjectConstructionLog>()
                        .eq(ProjectConstructionLog::getDetailId, detailId)
                        .orderByDesc(ProjectConstructionLog::getConstructionDate)
                        .orderByDesc(ProjectConstructionLog::getId));
        // 批量加载各历史记录留痕的附件
        Map<Long, List<ProjectConstructionAttachment>> attachmentMap = Map.of();
        if (!logs.isEmpty()) {
            List<Long> logIds = logs.stream().map(ProjectConstructionLog::getId).toList();
            attachmentMap = constructionAttachmentMapper.selectList(
                            new LambdaQueryWrapper<ProjectConstructionAttachment>()
                                    .in(ProjectConstructionAttachment::getLogId, logIds)
                                    .orderByAsc(ProjectConstructionAttachment::getId))
                    .stream()
                    .collect(Collectors.groupingBy(ProjectConstructionAttachment::getLogId));
        }
        List<ConstructionLogVO> result = new ArrayList<>(logs.size());
        for (ProjectConstructionLog log : logs) {
            ConstructionLogVO vo = toLogVO(log);
            List<ProjectConstructionAttachment> files = attachmentMap.getOrDefault(log.getId(), List.of());
            vo.setAttachments(files.stream().map(this::toAttachmentVO).toList());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDetailLog(ConstructionLogEditDTO dto) {
        ProjectApproval approval = approvalMapper.selectById(dto.getApprovalId());
        if (approval == null) {
            throw new BizException("立项信息不存在或已被删除");
        }
        if (!Constants.APPROVAL_STATUS_APPROVED.equals(approval.getStatus())) {
            throw new BizException("仅审核通过的立项可维护施工信息");
        }
        ProjectApprovalDetail detail = detailMapper.selectById(dto.getDetailId());
        if (detail == null) {
            throw new BizException("施工明细不存在或已被删除");
        }
        ProjectApprovalDeclare declare = declareMapper.selectById(detail.getDeclareId());
        if (declare == null || !dto.getApprovalId().equals(declare.getApprovalId())) {
            throw new BizException("施工明细不属于该立项");
        }

        String inspectionStatus = trimToNull(dto.getInspectionStatus());
        if (!Constants.CONSTRUCTION_INSPECTION_PASS.equals(inspectionStatus)
                && !Constants.CONSTRUCTION_INSPECTION_FAIL.equals(inspectionStatus)) {
            throw new BizException("巡检状态不合法");
        }
        String buildStatus = trimToNull(dto.getConstructionStatus());
        Set<String> allowed = Set.of(Constants.CONSTRUCTION_STATUS_NOT_STARTED,
                Constants.CONSTRUCTION_STATUS_IN_PROGRESS, Constants.CONSTRUCTION_STATUS_COMPLETED);
        if (buildStatus == null || !allowed.contains(buildStatus)) {
            throw new BizException("施工状态不合法");
        }

        // 每次编辑新增一条施工记录，保留历史留痕；最新一条作为详情展示
        ProjectConstructionLog log = new ProjectConstructionLog();
        log.setApprovalId(approval.getId());
        log.setDetailId(detail.getId());
        log.setConstructionDate(dto.getConstructionDate());
        log.setInspector(trimToNull(dto.getInspector()));
        log.setInspectionStatus(inspectionStatus);
        log.setConstructionStatus(buildStatus);
        log.setContent(trimToNull(dto.getContent()));
        log.setLogCode("SG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        log.setCreateBy(currentUser());
        log.setCreateTime(LocalDateTime.now());
        log.setDeleted(0);
        constructionLogMapper.insert(log);
        // 同步明细施工状态与日期，保证列表进度与一级聚合一致
        syncDetailConstruction(approval.getId(), detail, buildStatus, dto.getConstructionDate());
        // 附件按本次记录留痕保存，历史记录的附件不受影响
        saveLogAttachments(approval.getId(), detail.getId(), log.getId(), dto.getAttachments());
    }

    /** 同步三级明细施工状态：施工中回填开工日期，已完工回填完工日期 */
    private void syncDetailConstruction(Long approvalId, ProjectApprovalDetail detail, String buildStatus,
                                        LocalDate recordDate) {
        ProjectConstruction exist = constructionMapper.selectOne(new LambdaQueryWrapper<ProjectConstruction>()
                .eq(ProjectConstruction::getDetailId, detail.getId())
                .last("LIMIT 1"));
        ProjectConstruction entity = exist == null ? new ProjectConstruction() : exist;
        entity.setApprovalId(approvalId);
        entity.setDeclareId(detail.getDeclareId());
        entity.setDetailId(detail.getId());
        entity.setNodeLevel(Constants.CONSTRUCTION_LEVEL_DETAIL);
        entity.setBuildStatus(buildStatus);
        if (Constants.CONSTRUCTION_STATUS_IN_PROGRESS.equals(buildStatus)) {
            if (entity.getStartDate() == null) {
                entity.setStartDate(recordDate);
            }
            entity.setCompleteDate(null);
        } else if (Constants.CONSTRUCTION_STATUS_COMPLETED.equals(buildStatus)) {
            if (entity.getStartDate() == null) {
                entity.setStartDate(recordDate);
            }
            entity.setCompleteDate(recordDate);
        } else {
            entity.setStartDate(null);
            entity.setCompleteDate(null);
        }
        saveEntity(entity, exist == null);
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
        // 一级节点施工单位信息全部必填
        if (trimToNull(dto.getBuilder()) == null) {
            throw new BizException("施工单位名称不能为空");
        }
        if (trimToNull(dto.getContractorName()) == null) {
            throw new BizException("承包人姓名不能为空");
        }
        if (trimToNull(dto.getContractorPhone()) == null) {
            throw new BizException("承包人联系电话不能为空");
        }
        if (trimToNull(dto.getLeaderName()) == null) {
            throw new BizException("负责人姓名不能为空");
        }
        if (trimToNull(dto.getLeaderPhone()) == null) {
            throw new BizException("负责人联系电话不能为空");
        }

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
        saveAttachments(approvalId, null, dto.getAttachments());
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
        node.setSource(approval.getProjectType());
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
    private void saveAttachments(Long approvalId, Long detailId,
                                 List<ConstructionEditDTO.AttachmentItem> attachments) {
        if (attachments == null) {
            // 未传附件列表视为不修改附件
            return;
        }
        LambdaQueryWrapper<ProjectConstructionAttachment> scope = new LambdaQueryWrapper<ProjectConstructionAttachment>()
                .eq(ProjectConstructionAttachment::getApprovalId, approvalId);
        if (detailId == null) {
            scope.isNull(ProjectConstructionAttachment::getDetailId);
        } else {
            scope.eq(ProjectConstructionAttachment::getDetailId, detailId);
        }
        List<ProjectConstructionAttachment> existList = constructionAttachmentMapper.selectList(scope);
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
                    throw new BizException("施工资料附件不存在或不属于当前施工记录");
                }
                // 三级日志留痕附件（logId 非空）不属于项目/节点级范围，保留不动但前端不应回显它
                if (att.getLogId() != null) {
                    continue;
                }
                fillAttachment(att, item);
                constructionAttachmentMapper.updateById(att);
                continue;
            }
            ProjectConstructionAttachment att = new ProjectConstructionAttachment();
            att.setApprovalId(approvalId);
            att.setDetailId(detailId);
            fillAttachment(att, item);
            att.setCreateBy(currentUser());
            att.setCreateTime(LocalDateTime.now());
            att.setDeleted(0);
            constructionAttachmentMapper.insert(att);
        }
    }

    /**
     * 三级施工记录附件留痕保存：本次提交的附件全部复制为新记录，历史记录附件保持不变。
     * 已有附件仅作为文件信息（名称/key/桶/大小）来源，避免历史记录被覆盖。
     */
    private void saveLogAttachments(Long approvalId, Long detailId, Long logId,
                                    List<ConstructionEditDTO.AttachmentItem> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (ConstructionEditDTO.AttachmentItem item : attachments) {
            ProjectConstructionAttachment att = new ProjectConstructionAttachment();
            if (item.getId() != null) {
                ProjectConstructionAttachment source = constructionAttachmentMapper.selectById(item.getId());
                if (source == null || !approvalId.equals(source.getApprovalId())
                        || !java.util.Objects.equals(detailId, source.getDetailId())) {
                    throw new BizException("施工资料附件不存在或不属于当前施工记录");
                }
                att.setFileName(source.getFileName());
                att.setFileKey(source.getFileKey());
                att.setBucket(source.getBucket());
                att.setFileSize(source.getFileSize());
            }
            att.setApprovalId(approvalId);
            att.setDetailId(detailId);
            att.setLogId(logId);
            fillAttachment(att, item);
            if (att.getFileKey() == null) {
                continue;
            }
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
        vo.setDetailId(log.getDetailId());
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

package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.PageVO;
import com.msjs.project.dto.AcceptDetailDTO;
import com.msjs.project.dto.AcceptNodeDTO;
import com.msjs.project.dto.AcceptRecordUpdateDTO;
import com.msjs.project.dto.ArchiveDTO;
import com.msjs.project.entity.ProjectAcceptance;
import com.msjs.project.entity.ProjectAcceptanceAttachment;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.ProjectApprovalAttachment;
import com.msjs.project.entity.ProjectApprovalDeclare;
import com.msjs.project.entity.ProjectApprovalDetail;
import com.msjs.project.entity.ProjectArchive;
import com.msjs.project.entity.ProjectConstruction;
import com.msjs.project.mapper.ProjectAcceptanceAttachmentMapper;
import com.msjs.project.mapper.ProjectAcceptanceMapper;
import com.msjs.project.mapper.ProjectApprovalAttachmentMapper;
import com.msjs.project.mapper.ProjectApprovalDeclareMapper;
import com.msjs.project.mapper.ProjectApprovalDetailMapper;
import com.msjs.project.mapper.ProjectApprovalMapper;
import com.msjs.project.mapper.ProjectArchiveMapper;
import com.msjs.project.mapper.ProjectConstructionMapper;
import com.msjs.project.service.ProjectAcceptanceService;
import com.msjs.project.vo.AcceptanceNodeVO;
import com.msjs.project.vo.AcceptanceRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 验收管理服务实现
 * 数据来源：立项管理中审核通过且含申报明细的项目（立项通过即进入验收管理）
 * 三级树形：项目 → 申报内容 → 农户/公共基础设施明细
 * 验收进度 = 已验收明细数 / 全部明细数 × 100%
 * 归档规则：点击归档即把该项目写入档案管理，验收管理中不再显示
 */
@Service
@RequiredArgsConstructor
public class ProjectAcceptanceServiceImpl implements ProjectAcceptanceService {

    private final ProjectApprovalMapper approvalMapper;
    private final ProjectApprovalDeclareMapper declareMapper;
    private final ProjectApprovalDetailMapper detailMapper;
    private final ProjectApprovalAttachmentMapper approvalAttachmentMapper;
    private final ProjectAcceptanceMapper acceptanceMapper;
    private final ProjectAcceptanceAttachmentMapper acceptanceAttachmentMapper;
    private final ProjectArchiveMapper archiveMapper;
    private final ProjectConstructionMapper constructionMapper;

    @Override
    public PageVO<AcceptanceNodeVO> page(long pageNum, long pageSize, String projectName,
                                         String projectCategory, String projectSource) {
        // 数据来源：审核通过的立项。立项通过后即进入验收管理（accept_status 为 NONE 或 APPLYING），
        // 与资金管理、施工管理的数据口径保持一致，无需等待施工发起验收
        Page<ProjectApproval> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProjectApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_APPROVED)
                .in(ProjectApproval::getAcceptStatus,
                        Constants.APPROVAL_ACCEPT_NONE, Constants.APPROVAL_ACCEPT_APPLYING)
                .like(isNotBlank(projectName), ProjectApproval::getProjectName, trimToNull(projectName))
                .eq(isNotBlank(projectCategory), ProjectApproval::getProjectCategory, projectCategory)
                .eq(isNotBlank(projectSource), ProjectApproval::getProjectType, projectSource)
                .orderByDesc(ProjectApproval::getSubmitTime)
                .orderByDesc(ProjectApproval::getId);
        Page<ProjectApproval> result = approvalMapper.selectPage(page, wrapper);

        List<AcceptanceNodeVO> nodes = result.getRecords().isEmpty()
                ? Collections.emptyList()
                : buildTree(result.getRecords());

        PageVO<AcceptanceNodeVO> vo = new PageVO<>();
        vo.setRecords(nodes);
        vo.setTotal(result.getTotal());
        vo.setPageNum(result.getCurrent());
        vo.setPageSize(result.getSize());
        return vo;
    }

    @Override
    public AcceptanceNodeVO detail(Long approvalId) {
        ProjectApproval approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BizException("立项信息不存在或已被删除");
        }
        // 复用分页树组装逻辑，返回项目 → 申报 → 明细三级验收数据（含各级验收记录）
        return buildTree(Collections.singletonList(approval)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long approvalId, String applyBy) {
        ProjectApproval approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BizException("立项信息不存在");
        }
        if (!Constants.APPROVAL_STATUS_APPROVED.equals(approval.getStatus())) {
            throw new BizException("仅审核通过的立项可发起验收");
        }
        if (Constants.APPROVAL_ACCEPT_APPLYING.equals(approval.getAcceptStatus())) {
            throw new BizException("该项目已发起验收，请勿重复操作");
        }
        if (Constants.APPROVAL_ACCEPT_ARCHIVED.equals(approval.getAcceptStatus())) {
            throw new BizException("该项目已归档");
        }
        // 业务约束：只有全部明细施工已完工才允许发起验收
        if (!isAllConstructionCompleted(approval)) {
            throw new BizException("项目尚有明细未完工，不可发起验收");
        }
        approval.setAcceptStatus(Constants.APPROVAL_ACCEPT_APPLYING);
        approval.setAcceptApplyTime(LocalDateTime.now());
        approval.setAcceptApplyBy(trimToNull(applyBy));
        approval.setUpdateTime(LocalDateTime.now());
        approvalMapper.updateById(approval);
    }

    /**
     * 判断立项项目是否"全部明细施工已完工"
     * 条件：所有三级明细（t_project_approval_detail）都有对应的 t_project_construction 记录，
     * 且 build_status 全部为 COMPLETED
     */
    private boolean isAllConstructionCompleted(ProjectApproval approval) {
        List<ProjectApprovalDeclare> declares = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .eq(ProjectApprovalDeclare::getApprovalId, approval.getId()));
        if (declares.isEmpty()) {
            return false;
        }
        List<Long> declareIds = declares.stream().map(ProjectApprovalDeclare::getId).toList();
        List<ProjectApprovalDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDetail>()
                        .in(ProjectApprovalDetail::getDeclareId, declareIds));
        if (details.isEmpty()) {
            return false;
        }

        List<Long> detailIds = details.stream().map(ProjectApprovalDetail::getId).toList();
        Map<Long, String> buildStatusMap = constructionMapper.selectList(
                        new LambdaQueryWrapper<ProjectConstruction>()
                                .in(ProjectConstruction::getDetailId, detailIds)
                                .eq(ProjectConstruction::getNodeLevel, 3))
                .stream()
                .collect(Collectors.toMap(ProjectConstruction::getDetailId,
                        c -> c.getBuildStatus() == null ? "" : c.getBuildStatus(),
                        (a, b) -> a));

        // 每个明细都必须有施工记录且状态为 COMPLETED
        for (Long detailId : detailIds) {
            String status = buildStatusMap.get(detailId);
            if (!Constants.CONSTRUCTION_STATUS_COMPLETED.equals(status)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 组装三级验收树，批量查询避免 N+1
     */
    private List<AcceptanceNodeVO> buildTree(List<ProjectApproval> approvals) {
        List<Long> approvalIds = approvals.stream().map(ProjectApproval::getId).toList();

        // 一次性查询所有申报
        List<ProjectApprovalDeclare> declares = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .in(ProjectApprovalDeclare::getApprovalId, approvalIds)
                        .orderByAsc(ProjectApprovalDeclare::getSort)
                        .orderByAsc(ProjectApprovalDeclare::getId));

        // 一次性查询所有明细
        Map<Long, List<ProjectApprovalDetail>> detailMap = new HashMap<>();
        if (!declares.isEmpty()) {
            List<Long> declareIds = declares.stream().map(ProjectApprovalDeclare::getId).toList();
            List<ProjectApprovalDetail> details = detailMapper.selectList(
                    new LambdaQueryWrapper<ProjectApprovalDetail>()
                            .in(ProjectApprovalDetail::getDeclareId, declareIds)
                            .orderByAsc(ProjectApprovalDetail::getSort)
                            .orderByAsc(ProjectApprovalDetail::getId));
            detailMap = details.stream().collect(Collectors.groupingBy(ProjectApprovalDetail::getDeclareId));
        }

        // 一次性查询三级明细施工状态，口径与施工管理完全一致（t_project_construction 三级记录）
        Map<Long, String> detailBuildStatusMap = new HashMap<>();
        List<Long> allDetailIds = detailMap.values().stream()
                .flatMap(List::stream)
                .map(ProjectApprovalDetail::getId)
                .toList();
        if (!allDetailIds.isEmpty()) {
            constructionMapper.selectList(
                            new LambdaQueryWrapper<ProjectConstruction>()
                                    .in(ProjectConstruction::getDetailId, allDetailIds)
                                    .eq(ProjectConstruction::getNodeLevel, Constants.CONSTRUCTION_LEVEL_DETAIL))
                    .forEach(construction -> {
                        String buildStatus = construction.getBuildStatus();
                        detailBuildStatusMap.put(construction.getDetailId(),
                                buildStatus == null ? Constants.CONSTRUCTION_STATUS_NOT_STARTED : buildStatus);
                    });
        }

        // 一次性查询所有验收记录，按验收层级分组（每级只维护自己的验收状态）
        Map<Long, List<ProjectAcceptance>> detailAcceptMap = new HashMap<>();
        Map<Long, List<ProjectAcceptance>> projectAcceptMap = new HashMap<>();
        Map<Long, List<ProjectAcceptance>> declareAcceptMap = new HashMap<>();
        Map<Long, List<ProjectAcceptanceAttachment>> acceptAttachMap = new HashMap<>();
        List<ProjectAcceptance> acceptances = acceptanceMapper.selectList(
                new LambdaQueryWrapper<ProjectAcceptance>()
                        .in(ProjectAcceptance::getApprovalId, approvalIds)
                        .orderByDesc(ProjectAcceptance::getCreateTime)
                        .orderByDesc(ProjectAcceptance::getId));
        for (ProjectAcceptance acc : acceptances) {
            if (Constants.ACCEPTANCE_RECORD_PROJECT.equals(acc.getNodeType())) {
                projectAcceptMap.computeIfAbsent(acc.getApprovalId(), k -> new ArrayList<>()).add(acc);
            } else if (Constants.ACCEPTANCE_RECORD_DECLARE.equals(acc.getNodeType())) {
                declareAcceptMap.computeIfAbsent(acc.getDeclareId(), k -> new ArrayList<>()).add(acc);
            } else if (acc.getDetailId() != null) {
                detailAcceptMap.computeIfAbsent(acc.getDetailId(), k -> new ArrayList<>()).add(acc);
            }
        }
        if (!acceptances.isEmpty()) {
            List<Long> acceptanceIds = acceptances.stream().map(ProjectAcceptance::getId).toList();
            acceptanceAttachmentMapper.selectList(
                            new LambdaQueryWrapper<ProjectAcceptanceAttachment>()
                                    .in(ProjectAcceptanceAttachment::getAcceptanceId, acceptanceIds)
                                    .orderByAsc(ProjectAcceptanceAttachment::getId))
                    .stream()
                    .collect(Collectors.groupingBy(ProjectAcceptanceAttachment::getAcceptanceId))
                    .forEach(acceptAttachMap::put);
        }

        // 一次性查询归档记录
        Map<Long, ProjectArchive> archiveMap = archiveMapper.selectList(
                new LambdaQueryWrapper<ProjectArchive>()
                        .in(ProjectArchive::getApprovalId, approvalIds))
                .stream()
                .collect(Collectors.toMap(ProjectArchive::getApprovalId, a -> a, (a, b) -> a));

        // 一次性查询立项资料附件（一级查看弹窗使用）
        Map<Long, List<ProjectApprovalAttachment>> approvalAttachMap = approvalAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectApprovalAttachment>()
                                .in(ProjectApprovalAttachment::getApprovalId, approvalIds)
                                .orderByAsc(ProjectApprovalAttachment::getId))
                .stream()
                .collect(Collectors.groupingBy(ProjectApprovalAttachment::getApprovalId));

        // 组装树形结构
        Map<Long, List<ProjectApprovalDeclare>> declareByApprovalId = declares.stream()
                .collect(Collectors.groupingBy(ProjectApprovalDeclare::getApprovalId));

        List<AcceptanceNodeVO> roots = new ArrayList<>();
        for (ProjectApproval approval : approvals) {
            List<ProjectApprovalDeclare> approvalDeclares = declareByApprovalId.getOrDefault(
                    approval.getId(), Collections.emptyList());
            List<AcceptanceNodeVO> declareNodes = new ArrayList<>();
            int approvalTotalCount = 0;
            int approvalAcceptedCount = 0;
            int approvalStartedCount = 0;
            int approvalCompletedCount = 0;

            for (ProjectApprovalDeclare declare : approvalDeclares) {
                List<ProjectApprovalDetail> declareDetails = detailMap.getOrDefault(
                        declare.getId(), Collections.emptyList());
                List<AcceptanceNodeVO> detailNodes = new ArrayList<>();
                int declareTotalCount = declareDetails.size();
                int declareAcceptedCount = 0;
                int declareStartedCount = 0;
                int declareCompletedCount = 0;

                for (ProjectApprovalDetail detail : declareDetails) {
                    // 施工状态：无三级施工记录或未维护时按未开工处理
                    String buildStatus = detailBuildStatusMap.getOrDefault(
                            detail.getId(), Constants.CONSTRUCTION_STATUS_NOT_STARTED);
                    if (Constants.CONSTRUCTION_STATUS_COMPLETED.equals(buildStatus)) {
                        declareCompletedCount++;
                    } else if (Constants.CONSTRUCTION_STATUS_IN_PROGRESS.equals(buildStatus)) {
                        declareStartedCount++;
                    }

                    // 同一条明细可多次验收，倒序排列后第一条为最新一次
                    List<ProjectAcceptance> records = detailAcceptMap.getOrDefault(
                            detail.getId(), Collections.emptyList());
                    ProjectAcceptance latest = records.isEmpty() ? null : records.get(0);
                    // 三级验收状态：以最新一次验收记录为准（PENDING待验收 / ACCEPTED已验收 / REJECTED未通过）
                    String acceptStatus = latest != null && latest.getAcceptStatus() != null
                            ? latest.getAcceptStatus()
                            : Constants.ACCEPTANCE_STATUS_PENDING;
                    boolean isAccepted = Constants.ACCEPTANCE_STATUS_ACCEPTED.equals(acceptStatus);
                    if (isAccepted) {
                        declareAcceptedCount++;
                    }

                    AcceptanceNodeVO detailNode = new AcceptanceNodeVO();
                    detailNode.setNodeType(Constants.ACCEPTANCE_NODE_DETAIL);
                    detailNode.setApprovalId(approval.getId());
                    detailNode.setDeclareId(declare.getId());
                    detailNode.setDetailId(detail.getId());
                    detailNode.setId(detail.getId());
                    detailNode.setDetailType(detail.getDetailType());
                    detailNode.setCode(detail.getTargetCode());
                    detailNode.setName(detail.getTargetName());
                    detailNode.setSource(detail.getRegionName());
                    detailNode.setBudget(detail.getAmount());
                    detailNode.setTotalCount(1);
                    detailNode.setAcceptedCount(isAccepted ? 1 : 0);
                    detailNode.setPercent(isAccepted ? 100 : 0);
                    detailNode.setAcceptStatus(acceptStatus);
                    detailNode.setBuildStatus(buildStatus);
                    // 三级明细施工进度：未开工 0/1，施工中或已完工 1/1
                    int detailBuildNumerator = Constants.CONSTRUCTION_STATUS_NOT_STARTED.equals(buildStatus) ? 0 : 1;
                    detailNode.setBuildNumerator(detailBuildNumerator);
                    detailNode.setBuildPercent(detailBuildNumerator * 100);
                    // 基础信息展示最新一次验收情况
                    if (latest != null) {
                        detailNode.setAcceptTime(latest.getAcceptTime());
                        detailNode.setAcceptBy(latest.getAcceptBy());
                        detailNode.setAcceptResult(latest.getAcceptResult());
                        detailNode.setAcceptRemark(latest.getRemark());
                    }
                    detailNode.setAcceptanceAttachments(latest != null
                            ? acceptAttachMap.getOrDefault(latest.getId(), Collections.emptyList())
                            : Collections.emptyList());
                    detailNode.setAcceptanceRecords(toAcceptanceRecords(records, acceptAttachMap));
                    detailNode.setDetailRemark(detail.getRemark());
                    detailNode.setVillageName(detail.getRegionName());
                    detailNodes.add(detailNode);
                }

                declareTotalCount = detailNodes.size();
                approvalTotalCount += declareTotalCount;
                approvalAcceptedCount += declareAcceptedCount;
                approvalStartedCount += declareStartedCount;
                approvalCompletedCount += declareCompletedCount;

                AcceptanceNodeVO declareNode = new AcceptanceNodeVO();
                declareNode.setNodeType(Constants.ACCEPTANCE_NODE_DECLARE);
                declareNode.setApprovalId(approval.getId());
                declareNode.setDeclareId(declare.getId());
                declareNode.setId(declare.getId());
                declareNode.setCode(declare.getDeclareCode());
                declareNode.setName(declare.getDeclareName());
                declareNode.setCategory(declare.getDeclareCategory());
                declareNode.setSource(declare.getDeclareType());
                declareNode.setBudget(declare.getBudget());
                declareNode.setTotalCount(declareTotalCount);
                declareNode.setAcceptedCount(declareAcceptedCount);
                declareNode.setPercent(calcPercent(declareAcceptedCount, declareTotalCount));
                fillBuildProgress(declareNode, declareTotalCount, declareCompletedCount, declareStartedCount);
                declareNode.setDeclareContent(declare.getDeclareContent());
                declareNode.setTownName(declare.getTownName());
                declareNode.setVillageName(declare.getVillageName());
                declareNode.setChildren(detailNodes);
                // 二级验收状态与二级验收记录
                fillNodeAcceptance(declareNode,
                        declareAcceptMap.getOrDefault(declare.getId(), Collections.emptyList()),
                        acceptAttachMap);
                declareNodes.add(declareNode);
            }

            AcceptanceNodeVO projectNode = new AcceptanceNodeVO();
            projectNode.setNodeType(Constants.ACCEPTANCE_NODE_PROJECT);
            projectNode.setApprovalId(approval.getId());
            projectNode.setId(approval.getId());
            projectNode.setCode(approval.getApprovalCode());
            projectNode.setName(approval.getProjectName());
            projectNode.setCategory(approval.getProjectCategory());
            projectNode.setSource(approval.getProjectType());
            projectNode.setSubmitTime(approval.getSubmitTime());
            projectNode.setBudget(approval.getBudget());
            projectNode.setTotalCount(approvalTotalCount);
            projectNode.setAcceptedCount(approvalAcceptedCount);
            projectNode.setPercent(calcPercent(approvalAcceptedCount, approvalTotalCount));
            fillBuildProgress(projectNode, approvalTotalCount, approvalCompletedCount, approvalStartedCount);
            projectNode.setArchived(archiveMap.containsKey(approval.getId()));
            projectNode.setApprovalAttachments(approvalAttachMap.getOrDefault(
                    approval.getId(), Collections.emptyList()));
            projectNode.setChildren(declareNodes);
            // 一级验收状态与一级验收记录
            fillNodeAcceptance(projectNode,
                    projectAcceptMap.getOrDefault(approval.getId(), Collections.emptyList()),
                    acceptAttachMap);
            roots.add(projectNode);
        }
        return roots;
    }

    /**
     * 三级明细验收确认
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptDetail(AcceptDetailDTO dto) {
        // 校验立项存在且审核通过
        ProjectApproval approval = approvalMapper.selectById(dto.getApprovalId());
        if (approval == null) {
            throw new BizException("立项信息不存在");
        }
        if (!Constants.APPROVAL_STATUS_APPROVED.equals(approval.getStatus())) {
            throw new BizException("仅审核通过的立项可进行验收");
        }

        // 校验明细存在且属于该立项
        ProjectApprovalDetail detail = detailMapper.selectById(dto.getDetailId());
        if (detail == null) {
            throw new BizException("申报明细不存在或已被删除");
        }
        ProjectApprovalDeclare declare = declareMapper.selectById(detail.getDeclareId());
        if (declare == null || !declare.getApprovalId().equals(approval.getId())) {
            throw new BizException("申报明细不属于该立项项目");
        }

        LocalDateTime now = LocalDateTime.now();
        // 验收结果决定三级验收状态：未通过 → REJECTED，验收通过/整改后验收通过 → ACCEPTED
        String acceptStatus = Constants.ACCEPTANCE_RESULT_REJECTED.equals(dto.getAcceptResult())
                ? Constants.ACCEPTANCE_STATUS_REJECTED
                : Constants.ACCEPTANCE_STATUS_ACCEPTED;
        // 每次验收都新增一条记录，保留历史验收记录（未通过后整改可再次验收）
        ProjectAcceptance acceptance = new ProjectAcceptance();
        acceptance.setApprovalId(approval.getId());
        acceptance.setDeclareId(detail.getDeclareId());
        acceptance.setDetailId(detail.getId());
        acceptance.setNodeType(Constants.ACCEPTANCE_RECORD_DETAIL);
        acceptance.setAcceptStatus(acceptStatus);
        acceptance.setAcceptTime(dto.getAcceptTime() != null ? dto.getAcceptTime() : now);
        acceptance.setAcceptBy(trimToNull(dto.getAcceptBy()));
        acceptance.setAcceptResult(trimToNull(dto.getAcceptResult()));
        acceptance.setRemark(trimToNull(dto.getRemark()));
        acceptance.setCreateTime(now);
        acceptance.setUpdateTime(now);
        acceptanceMapper.insert(acceptance);

        // 保存验收资料附件
        saveAcceptanceAttachments(acceptance.getId(), dto.getAttachments(), now);
    }

    /**
     * 一级项目 / 二级申报验收
     * 每一级只维护自己的验收状态，不联动下级明细
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptNode(AcceptNodeDTO dto) {
        ProjectApproval approval = approvalMapper.selectById(dto.getApprovalId());
        if (approval == null) {
            throw new BizException("立项信息不存在");
        }
        if (!Constants.APPROVAL_STATUS_APPROVED.equals(approval.getStatus())) {
            throw new BizException("仅审核通过的立项可进行验收");
        }

        ProjectAcceptance record = new ProjectAcceptance();
        record.setApprovalId(approval.getId());
        if (Constants.ACCEPTANCE_NODE_PROJECT.equals(dto.getNodeType())) {
            record.setNodeType(Constants.ACCEPTANCE_RECORD_PROJECT);
        } else if (Constants.ACCEPTANCE_NODE_DECLARE.equals(dto.getNodeType())) {
            if (dto.getDeclareId() == null) {
                throw new BizException("申报ID不能为空");
            }
            ProjectApprovalDeclare declare = declareMapper.selectById(dto.getDeclareId());
            if (declare == null || !declare.getApprovalId().equals(approval.getId())) {
                throw new BizException("申报内容不属于该立项项目");
            }
            record.setNodeType(Constants.ACCEPTANCE_RECORD_DECLARE);
            record.setDeclareId(declare.getId());
        } else {
            throw new BizException("不支持的验收层级");
        }

        LocalDateTime now = LocalDateTime.now();
        // 验收结果决定该层级验收状态：未通过 → REJECTED，验收通过/整改后验收通过 → ACCEPTED
        String acceptStatus = Constants.ACCEPTANCE_RESULT_REJECTED.equals(dto.getAcceptResult())
                ? Constants.ACCEPTANCE_STATUS_REJECTED
                : Constants.ACCEPTANCE_STATUS_ACCEPTED;
        record.setAcceptStatus(acceptStatus);
        record.setAcceptTime(dto.getAcceptTime() != null ? dto.getAcceptTime() : now);
        record.setAcceptBy(trimToNull(dto.getAcceptBy()));
        record.setAcceptResult(trimToNull(dto.getAcceptResult()));
        record.setRemark(trimToNull(dto.getRemark()));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        acceptanceMapper.insert(record);

        // 保存验收资料附件
        saveAcceptanceAttachments(record.getId(), dto.getAttachments(), now);
    }

    /**
     * 编辑验收记录
     * 可修改验收结果、验收时间、验收人、验收备注及验收资料附件；验收状态随验收结果联动
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecord(AcceptRecordUpdateDTO dto) {
        ProjectAcceptance record = acceptanceMapper.selectById(dto.getId());
        if (record == null) {
            throw new BizException("验收记录不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        // 验收结果决定该层级验收状态：未通过 → REJECTED，验收通过/整改后验收通过 → ACCEPTED
        String acceptStatus = Constants.ACCEPTANCE_RESULT_REJECTED.equals(dto.getAcceptResult())
                ? Constants.ACCEPTANCE_STATUS_REJECTED
                : Constants.ACCEPTANCE_STATUS_ACCEPTED;
        record.setAcceptStatus(acceptStatus);
        record.setAcceptResult(trimToNull(dto.getAcceptResult()));
        record.setAcceptTime(dto.getAcceptTime() != null ? dto.getAcceptTime() : now);
        record.setAcceptBy(trimToNull(dto.getAcceptBy()));
        record.setRemark(trimToNull(dto.getRemark()));
        record.setUpdateTime(now);
        acceptanceMapper.updateById(record);

        // 附件整体替换：先移除原附件，再保存本次提交的附件
        acceptanceAttachmentMapper.delete(new LambdaQueryWrapper<ProjectAcceptanceAttachment>()
                .eq(ProjectAcceptanceAttachment::getAcceptanceId, record.getId()));
        saveAcceptanceAttachments(record.getId(), dto.getAttachments(), now);
    }

    /**
     * 项目归档
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(ArchiveDTO dto) {
        // 校验立项存在且审核通过
        ProjectApproval approval = approvalMapper.selectById(dto.getApprovalId());
        if (approval == null) {
            throw new BizException("立项信息不存在");
        }
        if (!Constants.APPROVAL_STATUS_APPROVED.equals(approval.getStatus())) {
            throw new BizException("仅审核通过的立项可进行归档");
        }

        // 校验未归档
        Long exists = archiveMapper.selectCount(
                new LambdaQueryWrapper<ProjectArchive>()
                        .eq(ProjectArchive::getApprovalId, dto.getApprovalId()));
        if (exists != null && exists > 0) {
            throw new BizException("该项目已归档，不可重复操作");
        }

        // 执行归档
        LocalDateTime now = LocalDateTime.now();
        ProjectArchive archive = new ProjectArchive();
        archive.setApprovalId(dto.getApprovalId());
        archive.setArchiveNo(generateArchiveNo());
        archive.setArchiveName(trimToNull(dto.getArchiveName()) != null
                ? dto.getArchiveName().trim()
                : approval.getProjectName());
        archive.setArchiveType(trimToNull(dto.getArchiveType()));
        archive.setRemark(trimToNull(dto.getRemark()));
        archive.setCreateBy(trimToNull(dto.getArchiveBy()));
        archive.setCreateTime(now);
        archiveMapper.insert(archive);

        // 同步更新立项验收状态为已归档
        approval.setAcceptStatus(Constants.APPROVAL_ACCEPT_ARCHIVED);
        approval.setUpdateTime(now);
        approvalMapper.updateById(approval);
    }

    /**
     * 回填项目 / 申报层级的验收状态（取最新一次记录，records 已按时间倒序）
     */
    private void fillNodeAcceptance(AcceptanceNodeVO node,
                                    List<ProjectAcceptance> records,
                                    Map<Long, List<ProjectAcceptanceAttachment>> attachMap) {
        ProjectAcceptance latest = records == null || records.isEmpty() ? null : records.get(0);
        node.setAcceptStatus(latest != null && latest.getAcceptStatus() != null
                ? latest.getAcceptStatus()
                : Constants.ACCEPTANCE_STATUS_PENDING);
        if (latest != null) {
            node.setAcceptResult(latest.getAcceptResult());
            node.setAcceptTime(latest.getAcceptTime());
            node.setAcceptBy(latest.getAcceptBy());
            node.setAcceptRemark(latest.getRemark());
        }
        node.setAcceptanceAttachments(latest != null
                ? attachMap.getOrDefault(latest.getId(), Collections.emptyList())
                : Collections.emptyList());
        node.setAcceptanceRecords(toAcceptanceRecords(records, attachMap));
    }

    /**
     * 验收记录转 VO，附件按记录回填（records 已按时间倒序）
     */
    private List<AcceptanceRecordVO> toAcceptanceRecords(List<ProjectAcceptance> records,
                                                         Map<Long, List<ProjectAcceptanceAttachment>> attachMap) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<AcceptanceRecordVO> list = new ArrayList<>(records.size());
        for (ProjectAcceptance record : records) {
            AcceptanceRecordVO vo = new AcceptanceRecordVO();
            vo.setId(record.getId());
            vo.setAcceptStatus(record.getAcceptStatus());
            vo.setAcceptResult(record.getAcceptResult());
            vo.setAcceptTime(record.getAcceptTime());
            vo.setAcceptBy(record.getAcceptBy());
            vo.setRemark(record.getRemark());
            vo.setAttachments(attachMap.getOrDefault(record.getId(), Collections.emptyList()));
            list.add(vo);
        }
        return list;
    }

    /**
     * 批量保存验收资料附件
     */
    private void saveAcceptanceAttachments(Long acceptanceId,
                                           List<AcceptDetailDTO.AttachmentItem> attachments,
                                           LocalDateTime now) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (AcceptDetailDTO.AttachmentItem item : attachments) {
            if (item.getFileKey() == null || item.getFileKey().isBlank()) {
                continue;
            }
            ProjectAcceptanceAttachment attachment = new ProjectAcceptanceAttachment();
            attachment.setAcceptanceId(acceptanceId);
            attachment.setAttachmentType(trimToNull(item.getAttachmentType()));
            attachment.setFileName(item.getFileName() != null ? item.getFileName().trim() : "");
            attachment.setFileKey(item.getFileKey().trim());
            attachment.setBucket(trimToNull(item.getBucket()));
            attachment.setFileSize(item.getFileSize());
            attachment.setRemark(trimToNull(item.getRemark()));
            attachment.setCreateTime(now);
            acceptanceAttachmentMapper.insert(attachment);
        }
    }

    /**
     * 计算进度百分比
     */
    private int calcPercent(int acceptedCount, int totalCount) {
        if (totalCount == 0) {
            return 0;
        }
        return (int) Math.round(acceptedCount * 100.0 / totalCount);
    }

    /**
     * 按三级明细聚合一级/二级施工状态与进度，与施工管理保持同一口径：
     * 有已完工 → 已完工（分子=完工数）；否则有施工中 → 施工中（分子=已开工数）；否则未开工
     */
    private void fillBuildProgress(AcceptanceNodeVO node, int totalCount,
                                   int completedCount, int startedCount) {
        String buildStatus;
        int numerator;
        if (completedCount > 0) {
            buildStatus = Constants.CONSTRUCTION_STATUS_COMPLETED;
            numerator = completedCount;
        } else if (startedCount > 0) {
            buildStatus = Constants.CONSTRUCTION_STATUS_IN_PROGRESS;
            numerator = startedCount;
        } else {
            buildStatus = Constants.CONSTRUCTION_STATUS_NOT_STARTED;
            numerator = 0;
        }
        node.setBuildStatus(buildStatus);
        node.setBuildNumerator(numerator);
        node.setBuildPercent(calcPercent(numerator, totalCount));
    }

    /**
     * 生成档案编号：DA + 时间戳 + 随机后缀，保证全局唯一
     * 表 t_project_archive 上有 uk_archive_no 唯一索引兜底
     */
    private String generateArchiveNo() {
        String timePart = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = String.format("%06d", (int) (Math.random() * 1_000_000));
        return "DA" + timePart + suffix;
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

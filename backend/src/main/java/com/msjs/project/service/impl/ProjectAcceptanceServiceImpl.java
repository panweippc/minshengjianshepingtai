package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.PageVO;
import com.msjs.project.dto.AcceptDetailDTO;
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
 * 数据来源：立项管理中审核通过且含申报明细的项目
 * 三级树形：项目 → 申报内容 → 农户/公共基础设施明细
 * 验收进度 = 已验收明细数 / 全部明细数 × 100%
 * 归档约束：验收进度达到 100% 方可归档
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
        // 数据来源：审核通过 + 已发起验收（accept_status = APPLYING）的立项
        Page<ProjectApproval> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProjectApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_APPROVED)
                .eq(ProjectApproval::getAcceptStatus, Constants.APPROVAL_ACCEPT_APPLYING)
                .like(isNotBlank(projectName), ProjectApproval::getProjectName, trimToNull(projectName))
                .eq(isNotBlank(projectCategory), ProjectApproval::getProjectCategory, projectCategory)
                .eq(isNotBlank(projectSource), ProjectApproval::getProjectType, projectSource)
                .orderByDesc(ProjectApproval::getAcceptApplyTime);
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

        // 一次性查询所有验收记录
        Map<Long, ProjectAcceptance> acceptMap = new HashMap<>();
        Map<Long, List<ProjectAcceptanceAttachment>> acceptAttachMap = new HashMap<>();
        Map<Long, Long> acceptByDetailId = new HashMap<>();
        List<ProjectAcceptance> acceptances = acceptanceMapper.selectList(
                new LambdaQueryWrapper<ProjectAcceptance>()
                        .in(ProjectAcceptance::getApprovalId, approvalIds));
        for (ProjectAcceptance acc : acceptances) {
            acceptByDetailId.put(acc.getDetailId(), acc.getId());
            acceptMap.put(acc.getDetailId(), acc);
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

            for (ProjectApprovalDeclare declare : approvalDeclares) {
                List<ProjectApprovalDetail> declareDetails = detailMap.getOrDefault(
                        declare.getId(), Collections.emptyList());
                List<AcceptanceNodeVO> detailNodes = new ArrayList<>();
                int declareTotalCount = declareDetails.size();
                int declareAcceptedCount = 0;

                for (ProjectApprovalDetail detail : declareDetails) {
                    ProjectAcceptance acceptance = acceptMap.get(detail.getId());
                    boolean isAccepted = acceptance != null
                            && Constants.ACCEPTANCE_STATUS_ACCEPTED.equals(acceptance.getAcceptStatus());
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
                    detailNode.setAcceptStatus(isAccepted
                            ? Constants.ACCEPTANCE_STATUS_ACCEPTED
                            : Constants.ACCEPTANCE_STATUS_PENDING);
                    if (acceptance != null) {
                        detailNode.setAcceptTime(acceptance.getAcceptTime());
                        detailNode.setAcceptBy(acceptance.getAcceptBy());
                        detailNode.setAcceptRemark(acceptance.getRemark());
                        Long accId = acceptByDetailId.get(detail.getId());
                        detailNode.setAcceptanceAttachments(acceptAttachMap.getOrDefault(
                                accId, Collections.emptyList()));
                    } else {
                        detailNode.setAcceptanceAttachments(Collections.emptyList());
                    }
                    detailNode.setDetailRemark(detail.getRemark());
                    detailNode.setVillageName(detail.getRegionName());
                    detailNodes.add(detailNode);
                }

                declareTotalCount = detailNodes.size();
                approvalTotalCount += declareTotalCount;
                approvalAcceptedCount += declareAcceptedCount;

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
                declareNode.setDeclareContent(declare.getDeclareContent());
                declareNode.setTownName(declare.getTownName());
                declareNode.setVillageName(declare.getVillageName());
                declareNode.setChildren(detailNodes);
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
            projectNode.setArchived(archiveMap.containsKey(approval.getId()));
            projectNode.setApprovalAttachments(approvalAttachMap.getOrDefault(
                    approval.getId(), Collections.emptyList()));
            projectNode.setChildren(declareNodes);
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
        ProjectAcceptance acceptance = acceptanceMapper.selectOne(
                new LambdaQueryWrapper<ProjectAcceptance>()
                        .eq(ProjectAcceptance::getDetailId, dto.getDetailId()));

        if (acceptance == null) {
            // 首次验收，新建记录
            acceptance = new ProjectAcceptance();
            acceptance.setApprovalId(approval.getId());
            acceptance.setDeclareId(detail.getDeclareId());
            acceptance.setDetailId(detail.getId());
            acceptance.setAcceptStatus(Constants.ACCEPTANCE_STATUS_ACCEPTED);
            acceptance.setAcceptTime(dto.getAcceptTime() != null ? dto.getAcceptTime() : now);
            acceptance.setAcceptBy(trimToNull(dto.getAcceptBy()));
            acceptance.setRemark(trimToNull(dto.getRemark()));
            acceptance.setCreateTime(now);
            acceptance.setUpdateTime(now);
            acceptanceMapper.insert(acceptance);
        } else {
            // 已存在记录（可能是 PENDING），更新为 ACCEPTED
            acceptance.setAcceptStatus(Constants.ACCEPTANCE_STATUS_ACCEPTED);
            acceptance.setAcceptTime(dto.getAcceptTime() != null ? dto.getAcceptTime() : now);
            acceptance.setAcceptBy(trimToNull(dto.getAcceptBy()));
            acceptance.setRemark(trimToNull(dto.getRemark()));
            acceptance.setUpdateTime(now);
            acceptanceMapper.updateById(acceptance);
        }

        // 保存验收资料附件
        saveAcceptanceAttachments(acceptance.getId(), dto.getAttachments(), now);
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

        // 校验验收进度 100%
        int totalCount = calcProjectDetailCount(dto.getApprovalId());
        int acceptedCount = calcProjectAcceptedCount(dto.getApprovalId());
        if (totalCount == 0) {
            throw new BizException("该项目无验收明细，不可归档");
        }
        if (acceptedCount < totalCount) {
            int percent = calcPercent(acceptedCount, totalCount);
            throw new BizException("验收进度未达 100%，当前进度 " + percent + "%，不可归档");
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
     * 统计项目明细总数
     */
    private int calcProjectDetailCount(Long approvalId) {
        List<Long> declareIds = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .eq(ProjectApprovalDeclare::getApprovalId, approvalId))
                .stream().map(ProjectApprovalDeclare::getId).toList();
        if (declareIds.isEmpty()) {
            return 0;
        }
        return detailMapper.selectCount(
                new LambdaQueryWrapper<ProjectApprovalDetail>()
                        .in(ProjectApprovalDetail::getDeclareId, declareIds)).intValue();
    }

    /**
     * 统计项目已验收明细数
     */
    private int calcProjectAcceptedCount(Long approvalId) {
        Long count = acceptanceMapper.selectCount(
                new LambdaQueryWrapper<ProjectAcceptance>()
                        .eq(ProjectAcceptance::getApprovalId, approvalId)
                        .eq(ProjectAcceptance::getAcceptStatus, Constants.ACCEPTANCE_STATUS_ACCEPTED));
        return count != null ? count.intValue() : 0;
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

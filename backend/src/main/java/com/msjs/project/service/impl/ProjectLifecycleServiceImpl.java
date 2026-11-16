package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.entity.Declaration;
import com.msjs.project.entity.DeclarationAttachment;
import com.msjs.project.entity.DeclarationDetailAuditLog;
import com.msjs.project.entity.DeclarationHousehold;
import com.msjs.project.entity.DeclarationPublicFacility;
import com.msjs.project.entity.ProjectAcceptance;
import com.msjs.project.entity.ProjectAcceptanceAttachment;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.ProjectApprovalAttachment;
import com.msjs.project.entity.ProjectApprovalDeclare;
import com.msjs.project.entity.ProjectApprovalDetail;
import com.msjs.project.entity.ProjectConstruction;
import com.msjs.project.entity.ProjectConstructionAttachment;
import com.msjs.project.entity.ProjectConstructionLog;
import com.msjs.project.entity.ProjectFund;
import com.msjs.project.entity.ProjectFundAttachment;
import com.msjs.project.mapper.DeclarationAttachmentMapper;
import com.msjs.project.mapper.DeclarationDetailAuditLogMapper;
import com.msjs.project.mapper.DeclarationHouseholdMapper;
import com.msjs.project.mapper.DeclarationMapper;
import com.msjs.project.mapper.DeclarationPublicFacilityMapper;
import com.msjs.project.mapper.ProjectAcceptanceAttachmentMapper;
import com.msjs.project.mapper.ProjectAcceptanceMapper;
import com.msjs.project.mapper.ProjectApprovalAttachmentMapper;
import com.msjs.project.mapper.ProjectApprovalDeclareMapper;
import com.msjs.project.mapper.ProjectApprovalDetailMapper;
import com.msjs.project.mapper.ProjectApprovalMapper;
import com.msjs.project.mapper.ProjectConstructionAttachmentMapper;
import com.msjs.project.mapper.ProjectConstructionLogMapper;
import com.msjs.project.mapper.ProjectConstructionMapper;
import com.msjs.project.mapper.ProjectFundAttachmentMapper;
import com.msjs.project.mapper.ProjectFundMapper;
import com.msjs.project.service.ProjectLifecycleService;
import com.msjs.project.vo.ProjectLifecycleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 农户 / 公共基础设施关联的项目全周期明细查询实现
 *
 * 关联链路（明细级）：
 *   农户：t_base_household.id ← t_declaration_household.household_id
 *         → t_project_approval_detail.household_id（存申报明细ID）
 *         → t_project_approval_declare（立项-申报关联）→ t_project_approval（立项主表）
 *   设施：t_base_facility.id ← t_declaration_public_facility.facility_id
 *         → t_project_approval_detail.facility_id（存申报明细ID）
 *   施工 / 资金 / 验收均按 t_project_approval_detail.id 作为 detail_id 关联，
 *   因此只取明细级记录，不返回项目级汇总数据；各阶段附件与记录一并返回。
 */
@Service
@RequiredArgsConstructor
public class ProjectLifecycleServiceImpl implements ProjectLifecycleService {

    /** 申报附件归属类型：申报单 */
    private static final String OWNER_DECLARATION = "declaration";
    /** 申报附件归属类型：农户明细 */
    private static final String OWNER_HOUSEHOLD = "household";
    /** 申报附件归属类型：公建明细 */
    private static final String OWNER_PUBLIC_FACILITY = "public_facility";

    private final DeclarationHouseholdMapper declarationHouseholdMapper;
    private final DeclarationPublicFacilityMapper declarationPublicFacilityMapper;
    private final DeclarationMapper declarationMapper;
    private final DeclarationAttachmentMapper declarationAttachmentMapper;
    private final DeclarationDetailAuditLogMapper declarationDetailAuditLogMapper;
    private final ProjectApprovalDetailMapper detailMapper;
    private final ProjectApprovalDeclareMapper approvalDeclareMapper;
    private final ProjectApprovalMapper approvalMapper;
    private final ProjectApprovalAttachmentMapper approvalAttachmentMapper;
    private final ProjectConstructionMapper constructionMapper;
    private final ProjectConstructionLogMapper constructionLogMapper;
    private final ProjectConstructionAttachmentMapper constructionAttachmentMapper;
    private final ProjectFundMapper fundMapper;
    private final ProjectFundAttachmentMapper fundAttachmentMapper;
    private final ProjectAcceptanceMapper acceptanceMapper;
    private final ProjectAcceptanceAttachmentMapper acceptanceAttachmentMapper;

    @Override
    public List<ProjectLifecycleVO> listByHousehold(Long householdId) {
        if (householdId == null) {
            return Collections.emptyList();
        }
        // 1. 该农户参与过的全部申报明细（包含尚未立项的，保证"从申报开始"）
        List<DeclarationHousehold> bills = declarationHouseholdMapper.selectList(
                new LambdaQueryWrapper<DeclarationHousehold>()
                        .eq(DeclarationHousehold::getHouseholdId, householdId)
                        .orderByDesc(DeclarationHousehold::getCreateTime)
                        .orderByDesc(DeclarationHousehold::getId));
        if (bills.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 申报明细 → 立项明细：t_project_approval_detail.household_id 存的是申报明细ID
        List<Long> billIds = bills.stream().map(DeclarationHousehold::getId).toList();
        List<ProjectApprovalDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDetail>()
                        .in(ProjectApprovalDetail::getHouseholdId, billIds));
        Map<Long, ProjectApprovalDetail> detailByBillId = new HashMap<>();
        for (ProjectApprovalDetail detail : details) {
            if (detail.getHouseholdId() != null) {
                detailByBillId.merge(detail.getHouseholdId(), detail, this::pickLatestDetail);
            }
        }

        List<BillInfo> billInfos = new ArrayList<>(bills.size());
        for (DeclarationHousehold bill : bills) {
            billInfos.add(new BillInfo(bill.getId(), bill.getDeclarationId(), bill.getAmount(),
                    bill.getRemark(), bill.getAuditStatus()));
        }
        return build(billInfos, detailByBillId, OWNER_HOUSEHOLD);
    }

    @Override
    public List<ProjectLifecycleVO> listByFacility(Long facilityId) {
        if (facilityId == null) {
            return Collections.emptyList();
        }
        // 1. 该设施参与过的全部申报明细（包含尚未立项的）
        List<DeclarationPublicFacility> bills = declarationPublicFacilityMapper.selectList(
                new LambdaQueryWrapper<DeclarationPublicFacility>()
                        .eq(DeclarationPublicFacility::getFacilityId, facilityId)
                        .orderByDesc(DeclarationPublicFacility::getCreateTime)
                        .orderByDesc(DeclarationPublicFacility::getId));
        if (bills.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 申报明细 → 立项明细：t_project_approval_detail.facility_id 存的是申报明细ID
        List<Long> billIds = bills.stream().map(DeclarationPublicFacility::getId).toList();
        List<ProjectApprovalDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDetail>()
                        .in(ProjectApprovalDetail::getFacilityId, billIds));
        Map<Long, ProjectApprovalDetail> detailByBillId = new HashMap<>();
        for (ProjectApprovalDetail detail : details) {
            if (detail.getFacilityId() != null) {
                detailByBillId.merge(detail.getFacilityId(), detail, this::pickLatestDetail);
            }
        }

        List<BillInfo> billInfos = new ArrayList<>(bills.size());
        for (DeclarationPublicFacility bill : bills) {
            billInfos.add(new BillInfo(bill.getId(), bill.getDeclarationId(), bill.getAmount(),
                    bill.getRemark(), bill.getAuditStatus()));
        }
        return build(billInfos, detailByBillId, OWNER_PUBLIC_FACILITY);
    }

    /**
     * 组装明细级全周期数据：申报 → 立项 → 施工 → 资金 → 验收
     * 所有阶段数据（含附件、记录）批量查询，避免 N+1
     *
     * @param billOwnerType 申报明细附件归属类型：household / public_facility
     */
    private List<ProjectLifecycleVO> build(List<BillInfo> bills,
                                           Map<Long, ProjectApprovalDetail> detailByBillId,
                                           String billOwnerType) {
        List<Long> billIds = bills.stream().map(BillInfo::billId).filter(Objects::nonNull).distinct().toList();
        List<Long> declarationIds = bills.stream().map(BillInfo::declarationId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Declaration> declarationMap = loadMap(declarationIds, declarationMapper::selectBatchIds,
                Declaration::getId);

        // 立项明细 → 立项-申报关联 → 立项主表
        Collection<ProjectApprovalDetail> details = detailByBillId.values();
        List<Long> approvalDeclareIds = details.stream().map(ProjectApprovalDetail::getDeclareId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, ProjectApprovalDeclare> approvalDeclareMap = loadMap(approvalDeclareIds,
                approvalDeclareMapper::selectBatchIds, ProjectApprovalDeclare::getId);

        List<Long> approvalIds = approvalDeclareMap.values().stream().map(ProjectApprovalDeclare::getApprovalId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, ProjectApproval> approvalMap = loadMap(approvalIds, approvalMapper::selectBatchIds,
                ProjectApproval::getId);

        List<Long> detailIds = details.stream().map(ProjectApprovalDetail::getId)
                .filter(Objects::nonNull).distinct().toList();

        // ============ 申报阶段附件 ============
        Map<String, List<ProjectLifecycleVO.LifecycleAttachmentVO>> declarationAttachMap =
                loadDeclarationAttachments(declarationIds, billIds);

        // ============ 申报阶段明细审核/修改记录 ============
        Map<Long, List<ProjectLifecycleVO.DeclarationAuditLogVO>> auditLogMap = new HashMap<>();
        if (!billIds.isEmpty()) {
            List<DeclarationDetailAuditLog> logs = declarationDetailAuditLogMapper.selectList(
                    new LambdaQueryWrapper<DeclarationDetailAuditLog>()
                            .eq(DeclarationDetailAuditLog::getDetailKind, billOwnerType)
                            .in(DeclarationDetailAuditLog::getDetailId, billIds)
                            .orderByDesc(DeclarationDetailAuditLog::getCreateTime)
                            .orderByDesc(DeclarationDetailAuditLog::getId));
            for (DeclarationDetailAuditLog log : logs) {
                ProjectLifecycleVO.DeclarationAuditLogVO item = new ProjectLifecycleVO.DeclarationAuditLogVO();
                item.setId(log.getId());
                item.setAction(log.getAction());
                item.setBeforeAuditStatus(log.getBeforeAuditStatus());
                item.setAfterAuditStatus(log.getAfterAuditStatus());
                item.setRemark(log.getRemark());
                item.setOperatorName(log.getOperatorName());
                item.setAttachmentChanged(log.getAttachmentChanged());
                item.setCreateTime(log.getCreateTime());
                auditLogMap.computeIfAbsent(log.getDetailId(), k -> new ArrayList<>()).add(item);
            }
        }

        // ============ 立项阶段附件 ============
        Map<Long, List<ProjectApprovalAttachment>> approvalAttachMap = new HashMap<>();
        if (!approvalIds.isEmpty()) {
            for (ProjectApprovalAttachment item : approvalAttachmentMapper.selectList(
                    new LambdaQueryWrapper<ProjectApprovalAttachment>()
                            .in(ProjectApprovalAttachment::getApprovalId, approvalIds)
                            .orderByAsc(ProjectApprovalAttachment::getId))) {
                approvalAttachMap.computeIfAbsent(item.getApprovalId(), k -> new ArrayList<>()).add(item);
            }
        }

        // ============ 施工 / 资金 / 验收（明细级） ============
        Map<Long, ProjectConstruction> constructionMap = new HashMap<>();
        Map<Long, List<ProjectConstructionLog>> constructionLogMap = new HashMap<>();
        Map<Long, List<ProjectConstructionAttachment>> constructionAttachMap = new HashMap<>();
        Map<Long, List<ProjectFund>> fundMap = new HashMap<>();
        Map<Long, List<ProjectFundAttachment>> fundAttachMap = new HashMap<>();
        List<Long> fundIds = new ArrayList<>();
        Map<Long, List<ProjectAcceptance>> acceptanceMap = new HashMap<>();
        Map<Long, List<ProjectAcceptanceAttachment>> acceptanceAttachMap = new HashMap<>();
        List<Long> acceptanceIds = new ArrayList<>();

        if (!detailIds.isEmpty()) {
            // 施工状态
            for (ProjectConstruction construction : constructionMapper.selectList(
                    new LambdaQueryWrapper<ProjectConstruction>()
                            .in(ProjectConstruction::getDetailId, detailIds))) {
                if (construction.getDetailId() != null) {
                    constructionMap.merge(construction.getDetailId(), construction, this::pickLatestConstruction);
                }
            }
            // 施工记录
            List<ProjectConstructionLog> logs = constructionLogMapper.selectList(
                    new LambdaQueryWrapper<ProjectConstructionLog>()
                            .in(ProjectConstructionLog::getDetailId, detailIds)
                            .orderByDesc(ProjectConstructionLog::getConstructionDate)
                            .orderByDesc(ProjectConstructionLog::getId));
            for (ProjectConstructionLog log : logs) {
                constructionLogMap.computeIfAbsent(log.getDetailId(), k -> new ArrayList<>()).add(log);
            }
            // 施工资料附件（明细级；含按记录留痕的附件）
            for (ProjectConstructionAttachment item : constructionAttachmentMapper.selectList(
                    new LambdaQueryWrapper<ProjectConstructionAttachment>()
                            .in(ProjectConstructionAttachment::getDetailId, detailIds)
                            .orderByAsc(ProjectConstructionAttachment::getId))) {
                if (item.getDetailId() != null) {
                    constructionAttachMap.computeIfAbsent(item.getDetailId(), k -> new ArrayList<>()).add(item);
                }
            }
            // 资金拨付记录
            List<ProjectFund> funds = fundMapper.selectList(
                    new LambdaQueryWrapper<ProjectFund>()
                            .in(ProjectFund::getDetailId, detailIds)
                            .orderByDesc(ProjectFund::getCreateTime)
                            .orderByDesc(ProjectFund::getId));
            for (ProjectFund fund : funds) {
                fundMap.computeIfAbsent(fund.getDetailId(), k -> new ArrayList<>()).add(fund);
                if (fund.getId() != null) {
                    fundIds.add(fund.getId());
                }
            }
            if (!fundIds.isEmpty()) {
                for (ProjectFundAttachment item : fundAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectFundAttachment>()
                                .in(ProjectFundAttachment::getFundId, fundIds)
                                .orderByAsc(ProjectFundAttachment::getId))) {
                    fundAttachMap.computeIfAbsent(item.getFundId(), k -> new ArrayList<>()).add(item);
                }
            }
            // 验收记录（保留全部历史记录）
            List<ProjectAcceptance> acceptances = acceptanceMapper.selectList(
                    new LambdaQueryWrapper<ProjectAcceptance>()
                            .in(ProjectAcceptance::getDetailId, detailIds)
                            .orderByDesc(ProjectAcceptance::getAcceptTime)
                            .orderByDesc(ProjectAcceptance::getId));
            for (ProjectAcceptance acceptance : acceptances) {
                acceptanceMap.computeIfAbsent(acceptance.getDetailId(), k -> new ArrayList<>()).add(acceptance);
                if (acceptance.getId() != null) {
                    acceptanceIds.add(acceptance.getId());
                }
            }
            if (!acceptanceIds.isEmpty()) {
                for (ProjectAcceptanceAttachment item : acceptanceAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectAcceptanceAttachment>()
                                .in(ProjectAcceptanceAttachment::getAcceptanceId, acceptanceIds)
                                .orderByAsc(ProjectAcceptanceAttachment::getId))) {
                    acceptanceAttachMap.computeIfAbsent(item.getAcceptanceId(), k -> new ArrayList<>()).add(item);
                }
            }
        }

        List<ProjectLifecycleVO> result = new ArrayList<>(bills.size());
        for (BillInfo bill : bills) {
            ProjectLifecycleVO vo = new ProjectLifecycleVO();
            vo.setDetailAuditStatus(bill.auditStatus());
            vo.setAmount(bill.amount());
            vo.setRemark(bill.remark());
            vo.setDeclarationAttachments(new ArrayList<>());
            vo.setDeclarationAuditLogs(new ArrayList<>());
            vo.setApprovalAttachments(new ArrayList<>());
            vo.setConstructionRecords(new ArrayList<>());
            vo.setConstructionAttachments(new ArrayList<>());
            vo.setFundRecords(new ArrayList<>());
            vo.setAcceptanceRecords(new ArrayList<>());

            // 申报阶段
            Declaration declaration = declarationMap.get(bill.declarationId());
            if (declaration != null) {
                vo.setDeclarationId(declaration.getId());
                vo.setDeclarationCode(declaration.getBusinessCode());
                vo.setDeclarationDate(declaration.getDeclarationDate());
                vo.setDeclarationTitle(declaration.getTitle());
                vo.setDeclarationType(declaration.getDeclarationType());
                vo.setDeclarationStatus(declaration.getStatus());
            }
            // 申报附件：申报单材料 + 当前农户/设施明细材料
            List<ProjectLifecycleVO.LifecycleAttachmentVO> declareAttachments = new ArrayList<>();
            declareAttachments.addAll(declarationAttachMap.getOrDefault(
                    OWNER_DECLARATION + "_" + bill.declarationId(), Collections.emptyList()));
            declareAttachments.addAll(declarationAttachMap.getOrDefault(
                    billOwnerType + "_" + bill.billId(), Collections.emptyList()));
            vo.setDeclarationAttachments(declareAttachments);
            vo.setDeclarationAuditLogs(auditLogMap.getOrDefault(bill.billId(), new ArrayList<>()));

            // 立项阶段
            ProjectApprovalDetail detail = detailByBillId.get(bill.billId());
            if (detail != null) {
                vo.setDetailId(detail.getId());
                vo.setDetailType(detail.getDetailType());
                ProjectApprovalDeclare approvalDeclare = approvalDeclareMap.get(detail.getDeclareId());
                if (approvalDeclare != null) {
                    ProjectApproval approval = approvalMap.get(approvalDeclare.getApprovalId());
                    if (approval != null) {
                        vo.setApprovalId(approval.getId());
                        vo.setApprovalCode(approval.getApprovalCode());
                        vo.setProjectName(approval.getProjectName());
                        vo.setProjectCategory(approval.getProjectCategory());
                        vo.setProjectType(approval.getProjectType());
                        vo.setApprovalStatus(approval.getStatus());
                        vo.setApprovalSubmitTime(approval.getSubmitTime());
                        vo.setRegionName(approval.getRegionName());
                        // 立项附件：项目级资料 + 该申报节点资料
                        List<ProjectLifecycleVO.LifecycleAttachmentVO> approvalAttachments = new ArrayList<>();
                        for (ProjectApprovalAttachment item : approvalAttachMap.getOrDefault(
                                approval.getId(), Collections.emptyList())) {
                            if (item.getDeclareId() != null
                                    && !item.getDeclareId().equals(approvalDeclare.getId())) {
                                continue;
                            }
                            String scope = item.getDeclareId() == null ? "项目资料" : "申报资料";
                            approvalAttachments.add(toAttachment(item.getId(), scope, item.getAttachmentType(),
                                    item.getFileName(), item.getFileKey(), item.getBucket(), item.getFileSize(),
                                    item.getRemark(), item.getCreateTime()));
                        }
                        vo.setApprovalAttachments(approvalAttachments);
                    }
                }

                // 施工阶段
                ProjectConstruction construction = constructionMap.get(detail.getId());
                if (construction != null) {
                    vo.setBuildStatus(construction.getBuildStatus());
                    vo.setStartDate(construction.getStartDate());
                    vo.setCompleteDate(construction.getCompleteDate());
                }
                // 施工记录 + 记录附件；未关联记录的施工资料单独返回
                List<ProjectLifecycleVO.ConstructionRecordVO> constructionRecords = new ArrayList<>();
                for (ProjectConstructionLog log : constructionLogMap.getOrDefault(
                        detail.getId(), Collections.emptyList())) {
                    ProjectLifecycleVO.ConstructionRecordVO record = new ProjectLifecycleVO.ConstructionRecordVO();
                    record.setId(log.getId());
                    record.setLogCode(log.getLogCode());
                    record.setConstructionDate(log.getConstructionDate());
                    record.setConstructionStatus(log.getConstructionStatus());
                    record.setInspector(log.getInspector());
                    record.setInspectionStatus(log.getInspectionStatus());
                    record.setContent(log.getContent());
                    record.setCreateBy(log.getCreateBy());
                    record.setCreateTime(log.getCreateTime());
                    record.setAttachments(new ArrayList<>());
                    constructionRecords.add(record);
                }
                List<ProjectLifecycleVO.LifecycleAttachmentVO> constructionAttachments = new ArrayList<>();
                for (ProjectConstructionAttachment item : constructionAttachMap.getOrDefault(
                        detail.getId(), Collections.emptyList())) {
                    ProjectLifecycleVO.LifecycleAttachmentVO attach = toAttachment(item.getId(), null,
                            item.getAttachmentType(), item.getFileName(), item.getFileKey(), item.getBucket(),
                            item.getFileSize(), item.getRemark(), item.getCreateTime());
                    if (item.getLogId() == null) {
                        constructionAttachments.add(attach);
                    } else {
                        constructionRecords.stream()
                                .filter(record -> item.getLogId().equals(record.getId()))
                                .findFirst()
                                .ifPresent(record -> record.getAttachments().add(attach));
                    }
                }
                vo.setConstructionRecords(constructionRecords);
                vo.setConstructionAttachments(constructionAttachments);

                // 资金阶段
                List<ProjectLifecycleVO.FundRecordVO> fundRecords = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                for (ProjectFund fund : fundMap.getOrDefault(detail.getId(), Collections.emptyList())) {
                    ProjectLifecycleVO.FundRecordVO record = new ProjectLifecycleVO.FundRecordVO();
                    record.setId(fund.getId());
                    record.setFundType(fund.getFundType());
                    record.setFundSource(fund.getFundSource());
                    record.setAmount(fund.getAmount());
                    record.setRemark(fund.getRemark());
                    record.setCreateBy(fund.getCreateBy());
                    record.setCreateTime(fund.getCreateTime());
                    record.setAttachments(new ArrayList<>());
                    fundRecords.add(record);
                    total = total.add(nullToZero(fund.getAmount()));
                }
                vo.setFundAmount(total);
                vo.setFundRecords(fundRecords);
                // 资金凭证附件
                for (ProjectLifecycleVO.FundRecordVO record : fundRecords) {
                    for (ProjectFundAttachment item : fundAttachMap.getOrDefault(
                            record.getId(), Collections.emptyList())) {
                        record.getAttachments().add(toAttachment(item.getId(), null, item.getAttachmentType(),
                                item.getFileName(), item.getFileKey(), item.getBucket(), item.getFileSize(),
                                item.getRemark(), item.getCreateTime()));
                    }
                }

                // 验收阶段
                List<ProjectLifecycleVO.AcceptanceRecordVO> acceptanceRecords = new ArrayList<>();
                for (ProjectAcceptance acceptance : acceptanceMap.getOrDefault(
                        detail.getId(), Collections.emptyList())) {
                    ProjectLifecycleVO.AcceptanceRecordVO record = new ProjectLifecycleVO.AcceptanceRecordVO();
                    record.setId(acceptance.getId());
                    record.setAcceptStatus(acceptance.getAcceptStatus());
                    record.setAcceptResult(acceptance.getAcceptResult());
                    record.setAcceptTime(acceptance.getAcceptTime());
                    record.setAcceptBy(acceptance.getAcceptBy());
                    record.setRemark(acceptance.getRemark());
                    record.setAttachments(new ArrayList<>());
                    for (ProjectAcceptanceAttachment item : acceptanceAttachMap.getOrDefault(
                            acceptance.getId(), Collections.emptyList())) {
                        record.getAttachments().add(toAttachment(item.getId(), null, item.getAttachmentType(),
                                item.getFileName(), item.getFileKey(), item.getBucket(), item.getFileSize(),
                                item.getRemark(), item.getCreateTime()));
                    }
                    acceptanceRecords.add(record);
                }
                vo.setAcceptanceRecords(acceptanceRecords);
                // 汇总字段取最新一条验收记录（已按验收时间倒序）
                if (!acceptanceRecords.isEmpty()) {
                    ProjectLifecycleVO.AcceptanceRecordVO latest = acceptanceRecords.get(0);
                    vo.setAcceptStatus(latest.getAcceptStatus());
                    vo.setAcceptResult(latest.getAcceptResult());
                    vo.setAcceptTime(latest.getAcceptTime());
                    vo.setAcceptBy(latest.getAcceptBy());
                    vo.setAcceptRemark(latest.getRemark());
                }
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 加载申报阶段附件：申报单材料（owner_type=declaration）+ 农户/公建明细材料
     * 返回 key = ownerType_ownerId
     */
    private Map<String, List<ProjectLifecycleVO.LifecycleAttachmentVO>> loadDeclarationAttachments(
            List<Long> declarationIds, List<Long> billIds) {
        List<Long> ownerIds = new ArrayList<>();
        ownerIds.addAll(declarationIds);
        ownerIds.addAll(billIds);
        Map<String, List<ProjectLifecycleVO.LifecycleAttachmentVO>> result = new HashMap<>();
        if (ownerIds.isEmpty()) {
            return result;
        }
        List<DeclarationAttachment> list = declarationAttachmentMapper.selectList(
                new LambdaQueryWrapper<DeclarationAttachment>()
                        .in(DeclarationAttachment::getOwnerType,
                                OWNER_DECLARATION, OWNER_HOUSEHOLD, OWNER_PUBLIC_FACILITY)
                        .in(DeclarationAttachment::getOwnerId, ownerIds)
                        .orderByAsc(DeclarationAttachment::getId));
        for (DeclarationAttachment item : list) {
            String scope = OWNER_DECLARATION.equals(item.getOwnerType()) ? "申报材料" : "明细材料";
            result.computeIfAbsent(item.getOwnerType() + "_" + item.getOwnerId(), k -> new ArrayList<>())
                    .add(toAttachment(item.getId(), scope, item.getAttachmentType(), item.getFileName(),
                            item.getFileKey(), item.getBucket(), item.getFileSize(), item.getRemark(),
                            item.getCreateTime()));
        }
        return result;
    }

    /** 组装统一附件 VO */
    private ProjectLifecycleVO.LifecycleAttachmentVO toAttachment(Long id, String scopeLabel,
                                                                  String attachmentType, String fileName,
                                                                  String fileKey, String bucket, Long fileSize,
                                                                  String remark, LocalDateTime createTime) {
        ProjectLifecycleVO.LifecycleAttachmentVO vo = new ProjectLifecycleVO.LifecycleAttachmentVO();
        vo.setId(id);
        vo.setScopeLabel(scopeLabel);
        vo.setAttachmentType(attachmentType);
        vo.setFileName(fileName);
        vo.setFileKey(fileKey);
        vo.setBucket(bucket);
        vo.setFileSize(fileSize);
        vo.setRemark(remark);
        vo.setCreateTime(createTime);
        return vo;
    }

    /** 批量查询并按主键转 Map */
    private <T> Map<Long, T> loadMap(List<Long> ids,
                                     java.util.function.Function<List<Long>, List<T>> loader,
                                     java.util.function.Function<T, Long> idGetter) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<T> items = loader.apply(ids);
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, T> map = new HashMap<>();
        for (T item : items) {
            Long id = idGetter.apply(item);
            if (id != null) {
                map.put(id, item);
            }
        }
        return map;
    }

    private ProjectApprovalDetail pickLatestDetail(ProjectApprovalDetail a, ProjectApprovalDetail b) {
        return compareId(a.getId(), b.getId()) >= 0 ? a : b;
    }

    private ProjectConstruction pickLatestConstruction(ProjectConstruction a, ProjectConstruction b) {
        int byTime = compareTime(a.getUpdateTime(), b.getUpdateTime());
        if (byTime != 0) {
            return byTime >= 0 ? a : b;
        }
        return compareId(a.getId(), b.getId()) >= 0 ? a : b;
    }

    private int compareTime(LocalDateTime a, LocalDateTime b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    private int compareId(Long a, Long b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 申报明细的公共字段（农户 / 设施共用） */
    private record BillInfo(Long billId, Long declarationId, BigDecimal amount,
                            String remark, String auditStatus) {
    }
}

package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.ArchiveEditDTO;
import com.msjs.project.entity.Declaration;
import com.msjs.project.entity.DeclarationAttachment;
import com.msjs.project.entity.DeclarationHousehold;
import com.msjs.project.entity.DeclarationPublicFacility;
import com.msjs.project.entity.ProjectAcceptance;
import com.msjs.project.entity.ProjectAcceptanceAttachment;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.ProjectApprovalAttachment;
import com.msjs.project.entity.ProjectApprovalDeclare;
import com.msjs.project.entity.ProjectApprovalDetail;
import com.msjs.project.entity.ProjectArchive;
import com.msjs.project.entity.ProjectConstructionAttachment;
import com.msjs.project.entity.ProjectFund;
import com.msjs.project.entity.ProjectFundAttachment;
import com.msjs.project.entity.SysDict;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.mapper.DeclarationAttachmentMapper;
import com.msjs.project.mapper.DeclarationHouseholdMapper;
import com.msjs.project.mapper.DeclarationMapper;
import com.msjs.project.mapper.DeclarationPublicFacilityMapper;
import com.msjs.project.mapper.ProjectAcceptanceAttachmentMapper;
import com.msjs.project.mapper.ProjectAcceptanceMapper;
import com.msjs.project.mapper.ProjectApprovalAttachmentMapper;
import com.msjs.project.mapper.ProjectApprovalDeclareMapper;
import com.msjs.project.mapper.ProjectApprovalDetailMapper;
import com.msjs.project.mapper.ProjectApprovalMapper;
import com.msjs.project.mapper.ProjectArchiveMapper;
import com.msjs.project.mapper.ProjectConstructionAttachmentMapper;
import com.msjs.project.mapper.ProjectFundAttachmentMapper;
import com.msjs.project.mapper.ProjectFundMapper;
import com.msjs.project.mapper.SysDictMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.service.ArchiveService;
import com.msjs.project.service.ProjectAcceptanceService;
import com.msjs.project.service.ProjectApprovalService;
import com.msjs.project.service.ProjectConstructionService;
import com.msjs.project.service.ProjectFundService;
import com.msjs.project.vo.ApprovalAttachmentVO;
import com.msjs.project.vo.ArchiveDetailVO;
import com.msjs.project.vo.ArchiveVO;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 档案管理服务实现
 * 数据来源：验收管理归档后的项目（t_project_archive）
 * 档案查看按六页签聚合：事项申报、立项、资金、施工、验收、档案
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    /** 导出压缩包内文件缓冲大小（8KB） */
    private static final int ZIP_BUFFER_SIZE = 8192;
    /** 默认存储桶（历史数据未记录桶名时使用正式归档桶） */
    private static final String DEFAULT_BUCKET = Constants.BUCKET_ARCHIVE_FORMAL;

    private final ProjectArchiveMapper archiveMapper;
    private final ProjectApprovalMapper approvalMapper;
    private final ProjectApprovalDeclareMapper declareMapper;
    private final ProjectApprovalDetailMapper approvalDetailMapper;
    private final ProjectApprovalAttachmentMapper approvalAttachmentMapper;
    private final DeclarationMapper declarationMapper;
    private final DeclarationHouseholdMapper declarationHouseholdMapper;
    private final DeclarationPublicFacilityMapper declarationPublicFacilityMapper;
    private final DeclarationAttachmentMapper declarationAttachmentMapper;
    private final ProjectFundMapper fundMapper;
    private final ProjectFundAttachmentMapper fundAttachmentMapper;
    private final ProjectConstructionAttachmentMapper constructionAttachmentMapper;
    private final ProjectAcceptanceMapper acceptanceMapper;
    private final ProjectAcceptanceAttachmentMapper acceptanceAttachmentMapper;
    private final SysDictMapper sysDictMapper;
    private final ProjectApprovalService approvalService;
    private final ProjectFundService fundService;
    private final ProjectConstructionService constructionService;
    private final ProjectAcceptanceService acceptanceService;
    private final MinioClient minioClient;
    private final SysRegionMapper regionMapper;

    @Override
    public PageVO<ArchiveVO> page(long pageNum, long pageSize, String projectName,
                                  String projectCategory, String projectType) {
        // 已归档项目 = 立项审核通过且验收状态为已归档
        Page<ProjectApproval> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProjectApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_APPROVED)
                .eq(ProjectApproval::getAcceptStatus, Constants.APPROVAL_ACCEPT_ARCHIVED)
                .like(isNotBlank(projectName), ProjectApproval::getProjectName, trimToNull(projectName))
                .eq(isNotBlank(projectCategory), ProjectApproval::getProjectCategory, projectCategory)
                .eq(isNotBlank(projectType), ProjectApproval::getProjectType, projectType)
                .orderByDesc(ProjectApproval::getUpdateTime)
                .orderByDesc(ProjectApproval::getId);
        applyApprovalRegionScope(wrapper);
        Page<ProjectApproval> result = approvalMapper.selectPage(page, wrapper);

        PageVO<ArchiveVO> vo = new PageVO<>();
        vo.setTotal(result.getTotal());
        vo.setPageNum(result.getCurrent());
        vo.setPageSize(result.getSize());
        if (result.getRecords().isEmpty()) {
            vo.setRecords(Collections.emptyList());
            return vo;
        }

        List<Long> approvalIds = result.getRecords().stream().map(ProjectApproval::getId).toList();
        Map<Long, ProjectArchive> archiveMap = archiveMapper.selectList(
                        new LambdaQueryWrapper<ProjectArchive>()
                                .in(ProjectArchive::getApprovalId, approvalIds))
                .stream()
                .collect(Collectors.toMap(ProjectArchive::getApprovalId, Function.identity(), (a, b) -> a));

        List<ArchiveVO> rows = new ArrayList<>();
        for (ProjectApproval approval : result.getRecords()) {
            ProjectArchive archive = archiveMap.get(approval.getId());
            if (archive == null) {
                continue;
            }
            rows.add(toArchiveVO(approval, archive));
        }
        vo.setRecords(rows);
        return vo;
    }

    @Override
    public ArchiveDetailVO detail(Long id) {
        ProjectArchive archive = requireArchive(id);
        ProjectApproval approval = approvalMapper.selectById(archive.getApprovalId());
        if (approval == null) {
            throw new BizException("立项信息不存在或已被删除");
        }

        ArchiveDetailVO detail = new ArchiveDetailVO();
        detail.setArchive(toArchiveVO(approval, archive));
        detail.setApproval(approvalService.detail(approval.getId()));
        detail.setApprovalAttachments(loadProjectAttachments(approval.getId()));
        detail.setDeclares(buildDeclares(approval));
        detail.setFund(fundService.approvalDetail(approval.getId()));
        detail.setConstruction(constructionService.detail(approval.getId()));
        detail.setAcceptance(acceptanceService.detail(approval.getId()));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ArchiveEditDTO dto) {
        ProjectArchive archive = requireArchive(dto.getId());
        archive.setStorageLocation(trimToNull(dto.getStorageLocation()));
        // 归档时间复用 create_time 字段（表内该字段语义即归档时间）
        archive.setCreateTime(dto.getArchiveTime());
        archiveMapper.updateById(archive);
    }

    @Override
    public void export(Long id, HttpServletResponse response) {
        ProjectArchive archive = requireArchive(id);
        ProjectApproval approval = approvalMapper.selectById(archive.getApprovalId());
        if (approval == null) {
            throw new BizException("立项信息不存在或已被删除");
        }
        String approvalCode = isNotBlank(approval.getApprovalCode()) ? approval.getApprovalCode().trim() : "项目";
        String projectName = isNotBlank(approval.getProjectName()) ? approval.getProjectName().trim() : "档案资料";
        String zipName = encodeFileName(approvalCode + "-" + projectName + "-档案资料-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".zip");
        response.setContentType("application/zip");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + zipName);

        // 已使用文件路径集合，避免同名附件覆盖
        Set<String> usedEntries = new HashSet<>();
        // 一次性加载四个附件类型字典，用于将 attachmentType(数字) 转成中文目录名
        Map<String, Map<String, String>> attachTypeDict = loadAttachmentTypeDict();
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            exportApprovalFiles(approval, zos, usedEntries, attachTypeDict);
            exportDeclarationFiles(approval, zos, usedEntries, attachTypeDict);
            exportFundFiles(approval, zos, usedEntries, attachTypeDict);
            exportConstructionFiles(approval, zos, usedEntries, attachTypeDict);
            exportAcceptanceFiles(approval, zos, usedEntries, attachTypeDict);
            zos.finish();
        } catch (Exception e) {
            log.error("导出项目档案失败: approvalId={}", approval.getId(), e);
            throw new BizException("导出档案失败，请稍后重试");
        }
    }

    /**
     * 加载附件类型字典：一次性查四个字典，返回 Map<dictType, Map<dictValue, dictLabel>>
     * 字典值是数字编码（如 "1"），导出时需要对应到中文标签（如 "拨款凭证"）作为目录名
     */
    private Map<String, Map<String, String>> loadAttachmentTypeDict() {
        List<String> dictTypes = List.of("li_xiang_fu_jian_lei_xing", "zi_jin_fu_jian_lei_xing",
                "shi_gong_fu_jian_lei_xing", "yan_shou_fu_jian_lei_xing");
        Map<String, Map<String, String>> result = new HashMap<>();
        sysDictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                        .in(SysDict::getDictType, dictTypes))
                .forEach(d -> result.computeIfAbsent(d.getDictType(), k -> new HashMap<>())
                        .put(d.getDictValue(), d.getDictLabel()));
        return result;
    }

    /** 根据字典类型+字典值，取中文标签；查不到返回默认值 */
    private String dictLabel(Map<String, Map<String, String>> dict, String dictType, String dictValue, String defaultValue) {
        if (!isNotBlank(dictValue)) {
            return defaultValue;
        }
        Map<String, String> inner = dict.get(dictType);
        if (inner == null) {
            return defaultValue;
        }
        String label = inner.get(dictValue);
        return isNotBlank(label) ? label : defaultValue;
    }

    // ==================== 事项申报 ====================

    /**
     * 组装事项申报数据：申报单基础信息 + 申报材料附件 + 农户/公共基础设施明细
     */
    private List<ArchiveDetailVO.ArchiveDeclareVO> buildDeclares(ProjectApproval approval) {
        List<ProjectApprovalDeclare> declares = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .eq(ProjectApprovalDeclare::getApprovalId, approval.getId())
                        .orderByAsc(ProjectApprovalDeclare::getSort)
                        .orderByAsc(ProjectApprovalDeclare::getId));
        if (declares.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> declarationIds = declares.stream()
                .map(ProjectApprovalDeclare::getDeclarationId)
                .filter(Objects::nonNull)
                .distinct().toList();
        Map<Long, Declaration> declarationMap = declarationIds.isEmpty()
                ? Collections.emptyMap()
                : declarationMapper.selectBatchIds(declarationIds).stream()
                        .collect(Collectors.toMap(Declaration::getId, Function.identity(), (a, b) -> a));

        // 申报材料附件（t_project_approval_attachment.declare_id 指向申报关联）
        Map<Long, List<ProjectApprovalAttachment>> attachmentMap = approvalAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectApprovalAttachment>()
                                .eq(ProjectApprovalAttachment::getApprovalId, approval.getId())
                                .isNotNull(ProjectApprovalAttachment::getDeclareId)
                                .orderByAsc(ProjectApprovalAttachment::getId))
                .stream()
                .collect(Collectors.groupingBy(ProjectApprovalAttachment::getDeclareId));

        // 申报单原始申报材料附件（t_declaration_attachment.owner_type=declaration）
        Map<Long, List<DeclarationAttachment>> declarationAttachMap = declarationIds.isEmpty()
                ? Collections.emptyMap()
                : declarationAttachmentMapper.selectList(
                                new LambdaQueryWrapper<DeclarationAttachment>()
                                        .in(DeclarationAttachment::getOwnerId, declarationIds)
                                        .eq(DeclarationAttachment::getOwnerType, "declaration")
                                        .orderByAsc(DeclarationAttachment::getId))
                        .stream().collect(Collectors.groupingBy(DeclarationAttachment::getOwnerId));

        // 农户/设施明细快照（按申报单批量查询）
        Map<Long, List<DeclarationHousehold>> householdMap = Collections.emptyMap();
        Map<Long, List<DeclarationPublicFacility>> facilityMap = Collections.emptyMap();
        if (!declarationIds.isEmpty()) {
            householdMap = declarationHouseholdMapper.selectList(
                            new LambdaQueryWrapper<DeclarationHousehold>()
                                    .in(DeclarationHousehold::getDeclarationId, declarationIds)
                                    .orderByAsc(DeclarationHousehold::getId))
                    .stream().collect(Collectors.groupingBy(DeclarationHousehold::getDeclarationId));
            facilityMap = declarationPublicFacilityMapper.selectList(
                            new LambdaQueryWrapper<DeclarationPublicFacility>()
                                    .in(DeclarationPublicFacility::getDeclarationId, declarationIds)
                                    .orderByAsc(DeclarationPublicFacility::getId))
                    .stream().collect(Collectors.groupingBy(DeclarationPublicFacility::getDeclarationId));
        }

        // 明细附件（owner_type=household/public_facility）
        Map<String, List<DeclarationAttachment>> detailAttachMap = new HashMap<>();
        List<Long> householdDetailIds = householdMap.values().stream().flatMap(List::stream)
                .map(DeclarationHousehold::getId).toList();
        List<Long> facilityDetailIds = facilityMap.values().stream().flatMap(List::stream)
                .map(DeclarationPublicFacility::getId).toList();
        List<Long> ownerIds = new ArrayList<>(householdDetailIds);
        ownerIds.addAll(facilityDetailIds);
        if (!ownerIds.isEmpty()) {
            declarationAttachmentMapper.selectList(
                            new LambdaQueryWrapper<DeclarationAttachment>()
                                    .in(DeclarationAttachment::getOwnerId, ownerIds)
                                    .in(DeclarationAttachment::getOwnerType, "household", "public_facility")
                                    .orderByAsc(DeclarationAttachment::getId))
                    .forEach(item -> detailAttachMap
                            .computeIfAbsent(item.getOwnerType() + "_" + item.getOwnerId(), k -> new ArrayList<>())
                            .add(item));
        }

        List<ArchiveDetailVO.ArchiveDeclareVO> result = new ArrayList<>();
        for (ProjectApprovalDeclare declare : declares) {
            ArchiveDetailVO.ArchiveDeclareVO node = new ArchiveDetailVO.ArchiveDeclareVO();
            node.setId(declare.getId());
            node.setDeclarationId(declare.getDeclarationId());
            node.setDeclareCode(declare.getDeclareCode());
            node.setDeclareName(declare.getDeclareName());
            node.setDeclareType(declare.getDeclareType());
            node.setDeclareCategory(declare.getDeclareCategory());
            node.setAmount(declare.getBudget());
            node.setTownName(declare.getTownName());
            node.setVillageName(declare.getVillageName());
            node.setRemark(declare.getDeclareContent());

            Declaration declaration = declarationMap.get(declare.getDeclarationId());
            if (declaration != null) {
                node.setDeclarationDate(declaration.getDeclarationDate());
                if (isNotBlank(declaration.getTitle())) {
                    node.setDeclareName(declaration.getTitle());
                }
                node.setAmount(declaration.getAmount());
                node.setTownName(declaration.getTownName());
                node.setVillageName(declaration.getVillageGroupName());
                node.setRemark(declaration.getRemark());
            }

            // 附件 = 立项侧补传的申报材料附件 + 申报单原始材料附件（按 fileKey 去重，立项侧优先）
            List<ArchiveDetailVO.ArchiveAttachmentVO> declareAttachments = new ArrayList<>();
            Set<String> attachKeys = new HashSet<>();
            for (ArchiveDetailVO.ArchiveAttachmentVO vo : toAttachments(attachmentMap.get(declare.getId()))) {
                if (attachKeys.add(vo.getFileKey() != null ? vo.getFileKey() : "id_" + vo.getId())) {
                    declareAttachments.add(vo);
                }
            }
            for (ArchiveDetailVO.ArchiveAttachmentVO vo : toDeclareAttachments(
                    declarationAttachMap.get(declare.getDeclarationId()))) {
                if (attachKeys.add(vo.getFileKey() != null ? vo.getFileKey() : "id_" + vo.getId())) {
                    declareAttachments.add(vo);
                }
            }
            node.setAttachments(declareAttachments);
            node.setDetails(buildDeclareDetails(declare, householdMap, facilityMap, detailAttachMap));
            result.add(node);
        }
        return result;
    }

    /**
     * 组装单条申报的明细：以立项明细为准，关联到申报快照补全户主/身份证/联系电话等
     */
    private List<ArchiveDetailVO.ArchiveDeclareDetailVO> buildDeclareDetails(
            ProjectApprovalDeclare declare,
            Map<Long, List<DeclarationHousehold>> householdMap,
            Map<Long, List<DeclarationPublicFacility>> facilityMap,
            Map<String, List<DeclarationAttachment>> detailAttachMap) {
        List<ProjectApprovalDetail> details = approvalDetailMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDetail>()
                        .eq(ProjectApprovalDetail::getDeclareId, declare.getId())
                        .orderByAsc(ProjectApprovalDetail::getSort)
                        .orderByAsc(ProjectApprovalDetail::getId));
        if (details.isEmpty()) {
            return Collections.emptyList();
        }

        List<DeclarationHousehold> households = householdMap.getOrDefault(declare.getDeclarationId(),
                Collections.emptyList());
        List<DeclarationPublicFacility> facilities = facilityMap.getOrDefault(declare.getDeclarationId(),
                Collections.emptyList());

        List<ArchiveDetailVO.ArchiveDeclareDetailVO> rows = new ArrayList<>();
        for (ProjectApprovalDetail detail : details) {
            ArchiveDetailVO.ArchiveDeclareDetailVO row = new ArchiveDetailVO.ArchiveDeclareDetailVO();
            row.setId(detail.getId());
            row.setDetailType(detail.getDetailType());
            row.setCode(detail.getTargetCode());
            row.setName(detail.getTargetName());
            row.setRegionName(detail.getRegionName());
            row.setRemark(detail.getRemark());
            row.setAmount(detail.getAmount());

            String ownerType;
            Long ownerId;
            if ("HOUSEHOLD".equalsIgnoreCase(detail.getDetailType())) {
                ownerType = "household";
                DeclarationHousehold household = households.stream()
                        .filter(item -> Objects.equals(item.getHouseholdId(), detail.getTargetId()))
                        .findFirst().orElse(null);
                if (household != null) {
                    ownerId = household.getId();
                    row.setCode(isNotBlank(household.getSnapshotHouseholdNo())
                            ? household.getSnapshotHouseholdNo() : detail.getTargetCode());
                    row.setName(isNotBlank(household.getSnapshotHeadName())
                            ? household.getSnapshotHeadName() : detail.getTargetName());
                    row.setIdCard(household.getSnapshotHeadIdCard());
                    row.setPhone(household.getSnapshotContactPhone());
                    row.setTypeValue(household.getSnapshotHouseholdType());
                    row.setRegionName(isNotBlank(household.getSnapshotRegionName())
                            ? household.getSnapshotRegionName() : detail.getRegionName());
                    if (household.getAmount() != null) {
                        row.setAmount(household.getAmount());
                    }
                    if (isNotBlank(household.getRemark())) {
                        row.setRemark(household.getRemark());
                    }
                } else {
                    ownerId = null;
                }
            } else {
                ownerType = "public_facility";
                DeclarationPublicFacility facility = facilities.stream()
                        .filter(item -> Objects.equals(item.getFacilityId(), detail.getTargetId()))
                        .findFirst().orElse(null);
                if (facility != null) {
                    ownerId = facility.getId();
                    row.setCode(isNotBlank(facility.getSnapshotFacilityCode())
                            ? facility.getSnapshotFacilityCode() : detail.getTargetCode());
                    row.setName(isNotBlank(facility.getSnapshotFacilityName())
                            ? facility.getSnapshotFacilityName() : detail.getTargetName());
                    row.setTypeValue(facility.getSnapshotFacilityType());
                    row.setRegionName(isNotBlank(facility.getSnapshotRegionName())
                            ? facility.getSnapshotRegionName() : detail.getRegionName());
                    if (facility.getAmount() != null) {
                        row.setAmount(facility.getAmount());
                    }
                    if (isNotBlank(facility.getRemark())) {
                        row.setRemark(facility.getRemark());
                    }
                } else {
                    ownerId = null;
                }
            }
            row.setAttachments(ownerId == null
                    ? Collections.emptyList()
                    : toDeclareAttachments(detailAttachMap.get(ownerType + "_" + ownerId)));
            rows.add(row);
        }
        return rows;
    }

    // ==================== 导出资料 ====================

    /** 立项资料附件：项目级 → 申报材料（按申报单分目录） */
    private void exportApprovalFiles(ProjectApproval approval, ZipOutputStream zos, Set<String> usedEntries,
                                     Map<String, Map<String, String>> attachTypeDict) {
        List<ProjectApprovalAttachment> all = approvalAttachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalAttachment>()
                        .eq(ProjectApprovalAttachment::getApprovalId, approval.getId())
                        .orderByAsc(ProjectApprovalAttachment::getId));
        if (all.isEmpty()) {
            return;
        }
        // 预加载申报关联，用于给 declareId 非空附件按申报单分目录
        Map<Long, ProjectApprovalDeclare> declareMap = declareMapper.selectList(
                        new LambdaQueryWrapper<ProjectApprovalDeclare>()
                                .eq(ProjectApprovalDeclare::getApprovalId, approval.getId()))
                .stream().collect(Collectors.toMap(ProjectApprovalDeclare::getId, Function.identity(), (a, b) -> a));

        for (ProjectApprovalAttachment item : all) {
            String subFolder;
            if (item.getDeclareId() == null) {
                // 项目级立项附件 → 01-立项资料/立项书/{附件类型}/
                subFolder = "立项书";
            } else {
                ProjectApprovalDeclare declare = declareMap.get(item.getDeclareId());
                String label = declare != null
                        ? "申报" + declare.getDeclareCode() + "-" + (isNotBlank(declare.getDeclareName()) ? declare.getDeclareName() : "未命名")
                        : "申报材料";
                subFolder = "申报材料/" + sanitizeFileName(label);
            }
            // 第三层：附件类型字典中文（li_xiang_fu_jian_lei_xing）
            String typeFolder = sanitizeFileName(dictLabel(attachTypeDict, "li_xiang_fu_jian_lei_xing",
                    item.getAttachmentType(), "未分类"));
            addZipEntry(zos, usedEntries, "01-立项资料/" + subFolder + "/" + typeFolder, item.getFileName(),
                    item.getBucket(), item.getFileKey());
        }
    }

    /** 事项申报附件：按申报单 → 申报材料 / 明细（农户 / 公建） 分层（DeclarationAttachment 无 attachmentType 字段） */
    private void exportDeclarationFiles(ProjectApproval approval, ZipOutputStream zos, Set<String> usedEntries,
                                        Map<String, Map<String, String>> attachTypeDict) {
        List<ProjectApprovalDeclare> declares = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .eq(ProjectApprovalDeclare::getApprovalId, approval.getId())
                        .orderByAsc(ProjectApprovalDeclare::getSort)
                        .orderByAsc(ProjectApprovalDeclare::getId));
        if (declares.isEmpty()) {
            return;
        }
        List<Long> declarationIds = declares.stream().map(ProjectApprovalDeclare::getDeclarationId)
                .filter(Objects::nonNull).distinct().toList();
        if (declarationIds.isEmpty()) {
            return;
        }

        // 申报单原始材料附件
        Map<Long, List<DeclarationAttachment>> declareAttachMap = declarationAttachmentMapper.selectList(
                        new LambdaQueryWrapper<DeclarationAttachment>()
                                .in(DeclarationAttachment::getOwnerId, declarationIds)
                                .eq(DeclarationAttachment::getOwnerType, "declaration"))
                .stream().collect(Collectors.groupingBy(DeclarationAttachment::getOwnerId));

        // 农户 / 公建明细（用于按名称生成目录）
        List<DeclarationHousehold> households = declarationHouseholdMapper.selectList(
                new LambdaQueryWrapper<DeclarationHousehold>()
                        .in(DeclarationHousehold::getDeclarationId, declarationIds));
        List<DeclarationPublicFacility> facilities = declarationPublicFacilityMapper.selectList(
                new LambdaQueryWrapper<DeclarationPublicFacility>()
                        .in(DeclarationPublicFacility::getDeclarationId, declarationIds));

        // 明细附件：owner_type → 明细对象 → 附件列表
        Map<String, List<DeclarationAttachment>> detailAttachMap = new HashMap<>();
        declarationAttachmentMapper.selectList(
                        new LambdaQueryWrapper<DeclarationAttachment>()
                                .in(DeclarationAttachment::getOwnerType, "household", "public_facility"))
                .forEach(item -> detailAttachMap.computeIfAbsent(
                        item.getOwnerType() + "_" + item.getOwnerId(), k -> new ArrayList<>()).add(item));

        for (ProjectApprovalDeclare declare : declares) {
            if (declare.getDeclarationId() == null) {
                continue;
            }
            String declareLabel = "申报" + declare.getDeclareCode() + "-"
                    + (isNotBlank(declare.getDeclareName()) ? declare.getDeclareName() : "未命名");
            String declareFolder = "02-事项申报/" + sanitizeFileName(declareLabel);

            // 申报单原始材料附件
            for (DeclarationAttachment item : declareAttachMap.getOrDefault(declare.getDeclarationId(), Collections.emptyList())) {
                addZipEntry(zos, usedEntries, declareFolder + "/申报材料", item.getFileName(),
                        item.getBucket(), item.getFileKey());
            }

            // 农户明细附件
            for (DeclarationHousehold hh : households.stream()
                    .filter(h -> Objects.equals(h.getDeclarationId(), declare.getDeclarationId())).toList()) {
                String key = "household_" + hh.getId();
                String hhLabel = "农户-" + (isNotBlank(hh.getSnapshotHeadName()) ? hh.getSnapshotHeadName() : "未知");
                for (DeclarationAttachment item : detailAttachMap.getOrDefault(key, Collections.emptyList())) {
                    addZipEntry(zos, usedEntries, declareFolder + "/明细/" + sanitizeFileName(hhLabel),
                            item.getFileName(), item.getBucket(), item.getFileKey());
                }
            }
            // 公建明细附件
            for (DeclarationPublicFacility pf : facilities.stream()
                    .filter(f -> Objects.equals(f.getDeclarationId(), declare.getDeclarationId())).toList()) {
                String key = "public_facility_" + pf.getId();
                String pfLabel = "公建-" + (isNotBlank(pf.getSnapshotFacilityName()) ? pf.getSnapshotFacilityName() : "未知");
                for (DeclarationAttachment item : detailAttachMap.getOrDefault(key, Collections.emptyList())) {
                    addZipEntry(zos, usedEntries, declareFolder + "/明细/" + sanitizeFileName(pfLabel),
                            item.getFileName(), item.getBucket(), item.getFileKey());
                }
            }
        }
    }

    /** 资金凭证附件：按资金流水分目录 */
    private void exportFundFiles(ProjectApproval approval, ZipOutputStream zos, Set<String> usedEntries,
                                 Map<String, Map<String, String>> attachTypeDict) {
        List<ProjectFund> funds = fundMapper.selectList(
                new LambdaQueryWrapper<ProjectFund>()
                        .eq(ProjectFund::getApprovalId, approval.getId())
                        .orderByAsc(ProjectFund::getId));
        if (funds.isEmpty()) {
            log.debug("档案导出: approvalId={} 无资金记录，跳过资金附件", approval.getId());
            return;
        }
        Map<Long, List<ProjectFundAttachment>> attachMap = fundAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectFundAttachment>()
                                .in(ProjectFundAttachment::getFundId, funds.stream().map(ProjectFund::getId).toList()))
                .stream().collect(Collectors.groupingBy(ProjectFundAttachment::getFundId));

        for (ProjectFund fund : funds) {
            String label = "资金流水" + (isNotBlank(fund.getFundType()) ? "-" + fund.getFundType() : "")
                    + (isNotBlank(fund.getRemark()) ? "-" + fund.getRemark() : "");
            String subFolder = sanitizeFileName(label);
            for (ProjectFundAttachment item : attachMap.getOrDefault(fund.getId(), Collections.emptyList())) {
                // 第三层：附件类型字典中文（zi_jin_fu_jian_lei_xing）
                String typeFolder = sanitizeFileName(dictLabel(attachTypeDict, "zi_jin_fu_jian_lei_xing",
                        item.getAttachmentType(), "未分类"));
                addZipEntry(zos, usedEntries, "03-资金凭证/" + subFolder + "/" + typeFolder, item.getFileName(),
                        item.getBucket(), item.getFileKey());
            }
        }
    }

    /** 施工资料附件：项目级 / 明细（按明细名称分目录） */
    private void exportConstructionFiles(ProjectApproval approval, ZipOutputStream zos, Set<String> usedEntries,
                                         Map<String, Map<String, String>> attachTypeDict) {
        List<ProjectConstructionAttachment> all = constructionAttachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectConstructionAttachment>()
                        .eq(ProjectConstructionAttachment::getApprovalId, approval.getId())
                        .orderByAsc(ProjectConstructionAttachment::getId));
        if (all.isEmpty()) {
            log.debug("档案导出: approvalId={} 施工附件为空", approval.getId());
            return;
        }
        // 预加载明细（detail_id → 明细名称）
        List<Long> detailIds = all.stream().map(ProjectConstructionAttachment::getDetailId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, ProjectApprovalDetail> detailMap = detailIds.isEmpty()
                ? Collections.emptyMap()
                : approvalDetailMapper.selectBatchIds(detailIds).stream()
                        .collect(Collectors.toMap(ProjectApprovalDetail::getId, Function.identity(), (a, b) -> a));

        for (ProjectConstructionAttachment item : all) {
            String subFolder;
            if (item.getDetailId() == null) {
                subFolder = "项目级";
            } else {
                ProjectApprovalDetail d = detailMap.get(item.getDetailId());
                String dName = d != null && isNotBlank(d.getTargetName()) ? d.getTargetName() : "未知明细";
                subFolder = "明细-" + sanitizeFileName(dName);
            }
            // 第三层：附件类型字典中文（shi_gong_fu_jian_lei_xing）
            String typeFolder = sanitizeFileName(dictLabel(attachTypeDict, "shi_gong_fu_jian_lei_xing",
                    item.getAttachmentType(), "未分类"));
            addZipEntry(zos, usedEntries, "04-施工资料/" + subFolder + "/" + typeFolder, item.getFileName(),
                    item.getBucket(), item.getFileKey());
        }
    }

    /** 验收资料附件：按验收节点 → 验收记录（时间） → 附件类型分目录 */
    private void exportAcceptanceFiles(ProjectApproval approval, ZipOutputStream zos, Set<String> usedEntries,
                                       Map<String, Map<String, String>> attachTypeDict) {
        List<ProjectAcceptance> acceptances = acceptanceMapper.selectList(
                new LambdaQueryWrapper<ProjectAcceptance>()
                        .eq(ProjectAcceptance::getApprovalId, approval.getId())
                        .orderByAsc(ProjectAcceptance::getAcceptTime)
                        .orderByAsc(ProjectAcceptance::getId));
        if (acceptances.isEmpty()) {
            log.debug("档案导出: approvalId={} 无验收记录", approval.getId());
            return;
        }
        Map<Long, List<ProjectAcceptanceAttachment>> attachMap = acceptanceAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectAcceptanceAttachment>()
                                .in(ProjectAcceptanceAttachment::getAcceptanceId, acceptances.stream().map(ProjectAcceptance::getId).toList()))
                .stream().collect(Collectors.groupingBy(ProjectAcceptanceAttachment::getAcceptanceId));

        // 同一验收节点下，同一天可能多次验收（整改前/整改后），按 dateKey 记录次数用于追加 ①②③ 避免目录重名
        Map<String, Integer> dayCount = new HashMap<>();

        for (ProjectAcceptance acc : acceptances) {
            // 第一层：验收节点（PROJECT整体 / DECLARE申报 / DETAIL明细）
            String nodeLabel;
            if ("PROJECT".equals(acc.getNodeType())) {
                nodeLabel = "项目整体验收";
            } else if ("DECLARE".equals(acc.getNodeType())) {
                Long declareId = acc.getDeclareId();
                ProjectApprovalDeclare declare = declareId != null ? declareMapper.selectById(declareId) : null;
                nodeLabel = declare != null
                        ? "申报" + declare.getDeclareCode() + "-" + (isNotBlank(declare.getDeclareName()) ? declare.getDeclareName() : "")
                        : "申报验收";
            } else if ("DETAIL".equals(acc.getNodeType())) {
                Long detailId = acc.getDetailId();
                ProjectApprovalDetail d = detailId != null ? approvalDetailMapper.selectById(detailId) : null;
                nodeLabel = "明细-" + (d != null && isNotBlank(d.getTargetName()) ? d.getTargetName() : "");
            } else {
                nodeLabel = "验收记录";
            }
            // 第二层：验收记录时间（格式 yyyy-MM-dd）
            String dateStr = acc.getAcceptTime() != null
                    ? acc.getAcceptTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "未填日期";
            String dateKey = nodeLabel + "|" + dateStr;
            int seq = dayCount.merge(dateKey, 1, Integer::sum);
            String recordLabel = seq == 1
                    ? "验收记录-" + dateStr
                    : "验收记录-" + dateStr + "-" + (seq == 2 ? "复验" : "第" + seq + "次");
            // 第三层：附件类型字典中文（yan_shou_fu_jian_lei_xing）
            for (ProjectAcceptanceAttachment item : attachMap.getOrDefault(acc.getId(), Collections.emptyList())) {
                String typeFolder = sanitizeFileName(dictLabel(attachTypeDict, "yan_shou_fu_jian_lei_xing",
                        item.getAttachmentType(), "未分类"));
                // 组合：05-验收资料/{节点}/{验收记录-日期}/{附件类型}
                String folder = "05-验收资料/" + sanitizeFileName(nodeLabel) + "/" + sanitizeFileName(recordLabel);
                addZipEntry(zos, usedEntries, folder + "/" + typeFolder, item.getFileName(),
                        item.getBucket(), item.getFileKey());
            }
        }
    }

    /** 写入单个附件到压缩包，文件缺失仅告警不中断导出 */
    private void addZipEntry(ZipOutputStream zos, Set<String> usedEntries, String folder,
                             String fileName, String bucket, String fileKey) {
        if (!isNotBlank(fileKey)) {
            return;
        }
        String safeName = sanitizeFileName(isNotBlank(fileName) ? fileName : fileNameFromKey(fileKey));
        String entryName = uniqueEntryName(folder, safeName, usedEntries);
        String effectiveBucket = isNotBlank(bucket) ? bucket : DEFAULT_BUCKET;
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                .bucket(effectiveBucket)
                .object(fileKey)
                .build())) {
            zos.putNextEntry(new ZipEntry(entryName));
            byte[] buffer = new byte[ZIP_BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        } catch (Exception e) {
            log.warn("导出档案附件失败，已跳过: bucket={}, fileKey={}", effectiveBucket, fileKey, e);
        }
    }

    /** 生成压缩包内唯一路径，重名时追加序号 */
    private String uniqueEntryName(String folder, String fileName, Set<String> usedEntries) {
        String entryName = folder + "/" + fileName;
        int index = 1;
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String suffix = dotIndex > 0 ? fileName.substring(dotIndex) : "";
        while (!usedEntries.add(entryName)) {
            entryName = folder + "/" + baseName + "(" + index + ")" + suffix;
            index++;
        }
        return entryName;
    }

    /** 过滤压缩包路径非法字符 */
    private String sanitizeFileName(String fileName) {
        String safe = fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return safe.isEmpty() ? "未命名文件" : safe;
    }

    /** 从对象 key 取文件名 */
    private String fileNameFromKey(String fileKey) {
        int index = fileKey.lastIndexOf('/');
        return index >= 0 ? fileKey.substring(index + 1) : fileKey;
    }

    /** 响应头文件名编码（RFC5987） */
    private String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // ==================== 通用 ====================

    private ProjectArchive requireArchive(Long id) {
        ProjectArchive archive = id == null ? null : archiveMapper.selectById(id);
        if (archive == null) {
            throw new BizException("档案信息不存在或已被删除");
        }
        return archive;
    }

    private ArchiveVO toArchiveVO(ProjectApproval approval, ProjectArchive archive) {
        ArchiveVO vo = new ArchiveVO();
        vo.setId(archive.getId());
        vo.setApprovalId(approval.getId());
        vo.setArchiveNo(archive.getArchiveNo());
        vo.setStorageLocation(archive.getStorageLocation());
        vo.setArchiveTime(archive.getCreateTime());
        vo.setArchiveBy(archive.getCreateBy());
        vo.setApprovalCode(approval.getApprovalCode());
        vo.setProjectName(approval.getProjectName());
        vo.setProjectCategory(approval.getProjectCategory());
        vo.setProjectType(approval.getProjectType());
        vo.setLeader(approval.getLeader());
        return vo;
    }

    /** 项目级立项资料附件（declare_id 为空） */
    private List<ArchiveDetailVO.ArchiveAttachmentVO> loadProjectAttachments(Long approvalId) {
        return approvalService.attachments(approvalId).stream()
                .filter(item -> item.getDeclareId() == null)
                .map(this::toAttachment)
                .toList();
    }

    private List<ArchiveDetailVO.ArchiveAttachmentVO> toAttachments(List<ProjectApprovalAttachment> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(item -> {
            ArchiveDetailVO.ArchiveAttachmentVO vo = new ArchiveDetailVO.ArchiveAttachmentVO();
            vo.setId(item.getId());
            vo.setAttachmentType(item.getAttachmentType());
            vo.setFileName(item.getFileName());
            vo.setFileKey(item.getFileKey());
            vo.setBucket(item.getBucket());
            vo.setFileSize(item.getFileSize());
            vo.setRemark(item.getRemark());
            vo.setCreateTime(item.getCreateTime());
            return vo;
        }).toList();
    }

    private List<ArchiveDetailVO.ArchiveAttachmentVO> toDeclareAttachments(List<DeclarationAttachment> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(item -> {
            ArchiveDetailVO.ArchiveAttachmentVO vo = new ArchiveDetailVO.ArchiveAttachmentVO();
            vo.setId(item.getId());
            vo.setAttachmentType(item.getAttachmentType());
            vo.setFileName(item.getFileName());
            vo.setFileKey(item.getFileKey());
            vo.setBucket(item.getBucket());
            vo.setFileSize(item.getFileSize());
            vo.setRemark(item.getRemark());
            vo.setCreateTime(item.getCreateTime());
            return vo;
        }).toList();
    }

    private ArchiveDetailVO.ArchiveAttachmentVO toAttachment(ApprovalAttachmentVO item) {
        ArchiveDetailVO.ArchiveAttachmentVO vo = new ArchiveDetailVO.ArchiveAttachmentVO();
        vo.setId(item.getId());
        vo.setAttachmentType(item.getAttachmentType());
        vo.setFileName(item.getFileName());
        vo.setFileKey(item.getFileKey());
        vo.setBucket(item.getBucket());
        vo.setFileSize(item.getFileSize());
        vo.setRemark(item.getRemark());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 数据权限：立项所属乡镇 region_code = 当前登录人所属乡镇 */
    private void applyApprovalRegionScope(LambdaQueryWrapper<ProjectApproval> wrapper) {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null || !isNotBlank(loginUser.getRegionCode())) {
            return;
        }
        String currentCode = loginUser.getRegionCode().trim();
        SysRegion town = null;
        for (int i = 0; i < 6; i++) {
            SysRegion r = regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                    .eq(SysRegion::getCode, currentCode));
            if (r == null) break;
            boolean isTown = ("1".equals(r.getRegionType())
                    || (r.getLevel() != null && r.getLevel() == Constants.REGION_LEVEL_TOWN));
            if (isTown) { town = r; break; }
            if (!isNotBlank(r.getParentCode())) break;
            currentCode = r.getParentCode();
        }
        if (town != null) {
            wrapper.eq(ProjectApproval::getRegionCode, town.getCode());
        }
    }
}

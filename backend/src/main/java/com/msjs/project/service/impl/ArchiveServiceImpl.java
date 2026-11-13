package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.PageVO;
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
    private final ProjectApprovalService approvalService;
    private final ProjectFundService fundService;
    private final ProjectConstructionService constructionService;
    private final ProjectAcceptanceService acceptanceService;
    private final MinioClient minioClient;

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
        String projectName = isNotBlank(approval.getProjectName()) ? approval.getProjectName().trim() : "项目档案";
        String zipName = encodeFileName(projectName + "-档案资料.zip");
        response.setContentType("application/zip");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + zipName);

        // 已使用文件路径集合，避免同名附件覆盖
        Set<String> usedEntries = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            exportApprovalFiles(approval.getId(), zos, usedEntries);
            exportDeclarationFiles(approval.getId(), zos, usedEntries);
            exportFundFiles(approval.getId(), zos, usedEntries);
            exportConstructionFiles(approval.getId(), zos, usedEntries);
            exportAcceptanceFiles(approval.getId(), zos, usedEntries);
            zos.finish();
        } catch (Exception e) {
            log.error("导出项目档案失败: approvalId={}", approval.getId(), e);
            throw new BizException("导出档案失败，请稍后重试");
        }
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

            node.setAttachments(toAttachments(attachmentMap.get(declare.getId())));
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

    /** 立项资料附件（项目级 + 申报材料级） */
    private void exportApprovalFiles(Long approvalId, ZipOutputStream zos, Set<String> usedEntries) {
        approvalAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectApprovalAttachment>()
                                .eq(ProjectApprovalAttachment::getApprovalId, approvalId)
                                .orderByAsc(ProjectApprovalAttachment::getId))
                .forEach(item -> addZipEntry(zos, usedEntries, "01-立项资料", item.getFileName(),
                        item.getBucket(), item.getFileKey()));
    }

    /** 事项申报附件（申报单级 + 农户/设施明细级） */
    private void exportDeclarationFiles(Long approvalId, ZipOutputStream zos, Set<String> usedEntries) {
        List<Long> declarationIds = declareMapper.selectList(
                        new LambdaQueryWrapper<ProjectApprovalDeclare>()
                                .eq(ProjectApprovalDeclare::getApprovalId, approvalId))
                .stream().map(ProjectApprovalDeclare::getDeclarationId)
                .filter(Objects::nonNull).distinct().toList();
        if (declarationIds.isEmpty()) {
            return;
        }
        List<Long> ownerIds = new ArrayList<>(declarationIds);
        declarationHouseholdMapper.selectList(
                        new LambdaQueryWrapper<DeclarationHousehold>()
                                .in(DeclarationHousehold::getDeclarationId, declarationIds))
                .forEach(item -> ownerIds.add(item.getId()));
        declarationPublicFacilityMapper.selectList(
                        new LambdaQueryWrapper<DeclarationPublicFacility>()
                                .in(DeclarationPublicFacility::getDeclarationId, declarationIds))
                .forEach(item -> ownerIds.add(item.getId()));

        declarationAttachmentMapper.selectList(
                        new LambdaQueryWrapper<DeclarationAttachment>()
                                .in(DeclarationAttachment::getOwnerId, ownerIds)
                                .orderByAsc(DeclarationAttachment::getId))
                .forEach(item -> addZipEntry(zos, usedEntries, "02-事项申报", item.getFileName(),
                        item.getBucket(), item.getFileKey()));
    }

    /** 资金凭证附件 */
    private void exportFundFiles(Long approvalId, ZipOutputStream zos, Set<String> usedEntries) {
        List<Long> fundIds = fundMapper.selectList(
                        new LambdaQueryWrapper<ProjectFund>()
                                .eq(ProjectFund::getApprovalId, approvalId))
                .stream().map(ProjectFund::getId).toList();
        if (fundIds.isEmpty()) {
            return;
        }
        fundAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectFundAttachment>()
                                .in(ProjectFundAttachment::getFundId, fundIds)
                                .orderByAsc(ProjectFundAttachment::getId))
                .forEach(item -> addZipEntry(zos, usedEntries, "03-资金凭证", item.getFileName(),
                        item.getBucket(), item.getFileKey()));
    }

    /** 施工资料附件 */
    private void exportConstructionFiles(Long approvalId, ZipOutputStream zos, Set<String> usedEntries) {
        constructionAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectConstructionAttachment>()
                                .eq(ProjectConstructionAttachment::getApprovalId, approvalId)
                                .orderByAsc(ProjectConstructionAttachment::getId))
                .forEach(item -> addZipEntry(zos, usedEntries, "04-施工资料", item.getFileName(),
                        item.getBucket(), item.getFileKey()));
    }

    /** 验收资料附件 */
    private void exportAcceptanceFiles(Long approvalId, ZipOutputStream zos, Set<String> usedEntries) {
        List<Long> acceptanceIds = acceptanceMapper.selectList(
                        new LambdaQueryWrapper<ProjectAcceptance>()
                                .eq(ProjectAcceptance::getApprovalId, approvalId))
                .stream().map(ProjectAcceptance::getId).toList();
        if (acceptanceIds.isEmpty()) {
            return;
        }
        acceptanceAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectAcceptanceAttachment>()
                                .in(ProjectAcceptanceAttachment::getAcceptanceId, acceptanceIds)
                                .orderByAsc(ProjectAcceptanceAttachment::getId))
                .forEach(item -> addZipEntry(zos, usedEntries, "05-验收资料", item.getFileName(),
                        item.getBucket(), item.getFileKey()));
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
}

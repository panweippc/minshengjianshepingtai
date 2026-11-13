package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.DeclarationAuditDTO;
import com.msjs.project.dto.DeclarationDetailAuditDTO;
import com.msjs.project.dto.DeclarationDetailEditDTO;
import com.msjs.project.dto.DeclarationDetailRevertDTO;
import com.msjs.project.dto.DeclarationSaveDTO;
import com.msjs.project.dto.DeclarationSubmitDTO;
import com.msjs.project.entity.BaseFacility;
import com.msjs.project.entity.Declaration;
import com.msjs.project.entity.DeclarationAttachment;
import com.msjs.project.entity.DeclarationDetailAuditLog;
import com.msjs.project.entity.DeclarationHousehold;
import com.msjs.project.entity.DeclarationPublicFacility;
import com.msjs.project.entity.Household;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.entity.SysUser;
import com.msjs.project.mapper.BaseFacilityMapper;
import com.msjs.project.mapper.DeclarationAttachmentMapper;
import com.msjs.project.mapper.DeclarationDetailAuditLogMapper;
import com.msjs.project.mapper.DeclarationHouseholdMapper;
import com.msjs.project.mapper.DeclarationMapper;
import com.msjs.project.mapper.DeclarationPublicFacilityMapper;
import com.msjs.project.mapper.HouseholdMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.mapper.SysUserMapper;
import com.msjs.project.service.DeclarationService;
import com.msjs.project.vo.DeclarationDetailAuditLogVO;
import com.msjs.project.vo.DeclarationDetailVO;
import com.msjs.project.vo.UserRegionVO;
import com.msjs.project.vo.DeclarationStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;





import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 事项申报管理 Service 实现
 * 核心约束：
 * 1. 所属乡镇由登录人区划自动向上递归解析，前端只读回传，后端强校验防篡改
 * 2. 业务编号 business_code 保存时自动生成（前缀 DEC + 日期 + 4位序号），全局唯一，作废不回收
 * 3. 主表 + 明细 + 附件 同事务保存，保证一致性
 */
@Service
@RequiredArgsConstructor
public class DeclarationServiceImpl implements DeclarationService {

    private final DeclarationMapper declarationMapper;
    private final DeclarationHouseholdMapper householdMapper;
    private final DeclarationPublicFacilityMapper facilityMapper;
    private final DeclarationAttachmentMapper attachmentMapper;
    private final SysRegionMapper regionMapper;
    private final HouseholdMapper baseHouseholdMapper;
    private final BaseFacilityMapper baseFacilityMapper;
    private final SysUserMapper userMapper;
    private final DeclarationDetailAuditLogMapper auditLogMapper;

    /** 业务编号前缀 */
    private static final String CODE_PREFIX = "DEC";
    /** 业务编号日期格式 */
    private static final DateTimeFormatter CODE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 业务编号序号位数 */
    private static final int CODE_SEQ_LEN = 4;

    @Override
    public PageVO<DeclarationDetailVO> page(long pageNum, long pageSize, String title,
                                            String declarationType, String declarationCategory,
                                            String status, String townCode, String villageGroupCode) {
        Page<Declaration> page = new Page<>(pageNum, pageSize);
        // tab → 明细 auditStatus 匹配集合
        // 注意：approved 只放 auditStatus=approved（已立项 established 归已立项 tab），tab 语义必须严格互斥
        List<String> targetAuditStatuses = null;
        if ("submitted".equals(status)) targetAuditStatuses = Arrays.asList("unsubmitted", "pending");
        else if ("approved".equals(status)) targetAuditStatuses = Collections.singletonList("approved");
        else if ("rejected".equals(status)) targetAuditStatuses = Collections.singletonList("rejected");
        else if ("established".equals(status)) targetAuditStatuses = Collections.singletonList("established");

        String me = currentUser();
        LambdaQueryWrapper<Declaration> wrapper = new LambdaQueryWrapper<Declaration>()
                .eq(Declaration::getCreateBy, me)
                .like(isNotBlank(title), Declaration::getTitle, isNotBlank(title) ? title.trim() : null)
                .eq(isNotBlank(declarationType), Declaration::getDeclarationType, declarationType)
                .eq(isNotBlank(declarationCategory), Declaration::getDeclarationCategory, declarationCategory)
                .eq(targetAuditStatuses == null && isNotBlank(status), Declaration::getStatus, status)
                .eq(isNotBlank(townCode), Declaration::getTownCode, townCode)
                .eq(isNotBlank(villageGroupCode), Declaration::getVillageGroupCode, villageGroupCode);
        // tab 分类逻辑（audit_status 匹配）：搬到 Java 层，避免 .apply() 占位符坑
        if (targetAuditStatuses != null) {
            Set<Long> matchingDeclIds = collectDeclIdsByAuditStatus(targetAuditStatuses);
            if (matchingDeclIds.isEmpty()) {
                // 没有任何申报单匹配此 tab → 强制返回空（selectPage 用 IN (空) 会被 MP 过滤掉）
                matchingDeclIds.add(-1L);
            }
            wrapper.in(Declaration::getId, matchingDeclIds);
        }
        wrapper.orderByDesc(Declaration::getCreateTime);

        Page<Declaration> result = declarationMapper.selectPage(page, wrapper);
        List<DeclarationDetailVO> records = new ArrayList<>();
        // 用同一个 targetAuditStatuses 过滤子明细：只查属于本 tab 语义的 auditStatus，
        // 避免 mixed 场景下 approved tab 出现 rejected 子行、rejected tab 出现 approved 子行。
        // 若 status 参数为空（全部 tab），则 targetAuditStatuses=null，子明细全量返回。
        for (Declaration d : result.getRecords()) {
            records.add(buildListVO(d, targetAuditStatuses));
        }
        PageVO<DeclarationDetailVO> vo = new PageVO<>();
        vo.setRecords(records);
        vo.setTotal(result.getTotal());
        vo.setPageNum(pageNum);
        vo.setPageSize(pageSize);
        return vo;
    }

    @Override
    public DeclarationDetailVO detail(Long id) {
        Declaration d = declarationMapper.selectById(id);
        if (d == null) {
            throw new BizException("申报单不存在或已被删除");
        }
        DeclarationDetailVO vo = toSimpleVO(d);
        // 主表级附件（owner_type=declaration, owner_id=d.id）
        vo.setAttachments(listAttachments("declaration", d.getId()));

        if ("1".equals(d.getDeclarationCategory())) {
            // 农户明细（快照存最新档案数据，防止后续农户档案变更影响申报）
            List<DeclarationHousehold> hhList = householdMapper.selectList(
                    new LambdaQueryWrapper<DeclarationHousehold>()
                            .eq(DeclarationHousehold::getDeclarationId, d.getId())
                            .orderByAsc(DeclarationHousehold::getId));
            List<DeclarationDetailVO.HouseholdVO> hVOs = new ArrayList<>();
            for (DeclarationHousehold hh : hhList) {
                DeclarationDetailVO.HouseholdVO hvo = new DeclarationDetailVO.HouseholdVO();
                hvo.setId(hh.getId());
                hvo.setHouseholdId(hh.getHouseholdId());
                hvo.setSnapshotHouseholdNo(hh.getSnapshotHouseholdNo());
                hvo.setSnapshotHeadName(hh.getSnapshotHeadName());
                hvo.setSnapshotHouseholdNo(hh.getSnapshotHouseholdNo());
                hvo.setSnapshotHeadIdCard(hh.getSnapshotHeadIdCard());
                hvo.setSnapshotContactPhone(hh.getSnapshotContactPhone());
                hvo.setSnapshotHouseholdType(hh.getSnapshotHouseholdType());
                hvo.setSnapshotRegionName(hh.getSnapshotRegionName());
                hvo.setAmount(hh.getAmount());
                hvo.setRemark(hh.getRemark());
                hvo.setAuditStatus(hh.getAuditStatus());
                hvo.setAuditBy(hh.getAuditBy());
                hvo.setAuditTime(hh.getAuditTime() == null ? null : hh.getAuditTime().toString());
                hvo.setAuditRemark(hh.getAuditRemark());
                // 附件归属：owner_type=household, owner_id=明细ID（不是农户档案ID，因为一个农户可能在不同申报里出现多次）
                List<DeclarationDetailVO.AttachmentVO> haList = listAttachments("household", hh.getId());
                hvo.setAttachmentCount(haList.size());
                hvo.setAttachments(haList);
                hVOs.add(hvo);
            }
            fillHouseholdAuditByName(hVOs);
            vo.setHouseholds(hVOs);
        } else if ("2".equals(d.getDeclarationCategory())) {
            // 公建设施明细
            List<DeclarationPublicFacility> pfList = facilityMapper.selectList(
                    new LambdaQueryWrapper<DeclarationPublicFacility>()
                            .eq(DeclarationPublicFacility::getDeclarationId, d.getId())
                            .orderByAsc(DeclarationPublicFacility::getId));
            List<DeclarationDetailVO.FacilityVO> fVOs = new ArrayList<>();
            for (DeclarationPublicFacility pf : pfList) {
                DeclarationDetailVO.FacilityVO fvo = new DeclarationDetailVO.FacilityVO();
                fvo.setId(pf.getId());
                fvo.setFacilityId(pf.getFacilityId());
                fvo.setSnapshotFacilityCode(pf.getSnapshotFacilityCode());
                fvo.setSnapshotFacilityName(pf.getSnapshotFacilityName());
                fvo.setSnapshotFacilityType(pf.getSnapshotFacilityType());
                fvo.setSnapshotLocation(pf.getSnapshotLocation());
                fvo.setSnapshotRegionName(pf.getSnapshotRegionName());
                fvo.setAmount(pf.getAmount());
                fvo.setRemark(pf.getRemark());
                fvo.setAuditStatus(pf.getAuditStatus());
                fvo.setAuditBy(pf.getAuditBy());
                fvo.setAuditTime(pf.getAuditTime() == null ? null : pf.getAuditTime().toString());
                fvo.setAuditRemark(pf.getAuditRemark());
                // 附件归属：owner_type=public_facility, owner_id=明细ID
                List<DeclarationDetailVO.AttachmentVO> faList = listAttachments("public_facility", pf.getId());
                fvo.setAttachmentCount(faList.size());
                fvo.setAttachments(faList);
                fVOs.add(fvo);
            }
            // 批量填公建设施明细审核人姓名
            fillFacilityAuditByName(fVOs);
            vo.setFacilities(fVOs);
        }
        // 冗余审核人姓名（detail 场景单独填，因为 detail 调的是 toSimpleVO 不是 buildListVO）
        if (d.getRecipientUserId() != null) {
            SysUser r = userMapper.selectById(d.getRecipientUserId());
            vo.setRecipientRealName(r != null ? r.getRealName() : null);
        }
        if (d.getAuditBy() != null) {
            SysUser a = userMapper.selectById(d.getAuditBy());
            vo.setAuditByRealName(a != null ? a.getRealName() : null);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(DeclarationSaveDTO dto) {
        // 1. 登录人乡镇：后端自己解析，和前端回传值做一致性校验，防止前端篡改
        UserRegionVO userRegion = resolveUserTown();
        if (isNotBlank(dto.getTownCode()) && !dto.getTownCode().trim().equals(userRegion.getTownCode())) {
            throw new BizException("所属乡镇与当前登录人归属不一致，不允许修改");
        }
        // 2. 村组校验：必须存在且父级是登录人的乡镇
        SysRegion vgRegion = regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getCode, dto.getVillageGroupCode().trim()));
        if (vgRegion == null) {
            throw new BizException("所属村组不存在");
        }
        if (!userRegion.getTownCode().equals(vgRegion.getParentCode())) {
            throw new BizException("所选村组不在您的所属乡镇下");
        }

        // 3. 明细必填校验
        if ("1".equals(dto.getDeclarationCategory())) {
            if (dto.getHouseholdIds() == null || dto.getHouseholdIds().isEmpty()) {
                throw new BizException("到户项目必须至少勾选1户农户");
            }
        } else if ("2".equals(dto.getDeclarationCategory())) {
            if (dto.getFacilityIds() == null || dto.getFacilityIds().isEmpty()) {
                throw new BizException("公建项目必须至少勾选1个公共基础设施");
            }
        }

        // 4. 业务编号：优先用前端回传的（弹窗预生成），没传则自动生成；两种情况都校验唯一性
        String businessCode;
        if (dto.getBusinessCode() != null && !dto.getBusinessCode().isBlank()) {
            businessCode = dto.getBusinessCode().trim();
            if (!isBusinessCodeUnique(businessCode)) {
                throw new BizException("申报编号「" + businessCode + "」已存在，请联系管理员或刷新页面重新获取编号");
            }
        } else {
            businessCode = generateBusinessCode();
        }

        // 5. 保存主表
        Declaration declaration = new Declaration();
        declaration.setBusinessCode(businessCode);
        declaration.setDeclarationDate(dto.getDeclarationDate() != null ? dto.getDeclarationDate() : java.time.LocalDate.now());
        declaration.setTitle(trimToNull(dto.getTitle()));
        declaration.setTownCode(userRegion.getTownCode());
        declaration.setTownName(userRegion.getTownName());
        declaration.setVillageGroupCode(vgRegion.getCode());
        declaration.setVillageGroupName(vgRegion.getName());
        declaration.setDeclarationType(trimToNull(dto.getDeclarationType()));
        declaration.setDeclarationCategory(dto.getDeclarationCategory());
        declaration.setAmount(dto.getAmount());
        declaration.setRemark(trimToNull(dto.getRemark()));
        declaration.setStatus("draft");
        declaration.setCreateBy(currentUser());
        declaration.setCreateTime(LocalDateTime.now());
        declarationMapper.insert(declaration);

        // 6. 保存明细（快照从档案表查最新数据）
        saveHouseholdSnapshots(declaration.getId(), dto.getHouseholdIds(), vgRegion);
        saveFacilitySnapshots(declaration.getId(), dto.getFacilityIds(), vgRegion);

        // 7. 保存申报单级附件（owner_type=declaration）
        saveAttachments("declaration", declaration.getId(), dto.getAttachments(), null);

        return declaration.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(DeclarationSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("缺少主键ID");
        }
        Declaration exist = declarationMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("申报单不存在或已被删除");
        }
        // 仅已通过的申报单不允许编辑（已驳回的允许：用户修改驳回内容后重新提交）
        if ("approved".equals(exist.getStatus())) {
            throw new BizException("已通过的申报单不允许编辑");
        }

        // 乡镇不可修改
        UserRegionVO userRegion = resolveUserTown();
        if (!exist.getTownCode().equals(userRegion.getTownCode())) {
            throw new BizException("所属乡镇不可修改");
        }
        // 村组校验
        SysRegion vgRegion = regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getCode, dto.getVillageGroupCode().trim()));
        if (vgRegion == null || !userRegion.getTownCode().equals(vgRegion.getParentCode())) {
            throw new BizException("所属村组不在您的所属乡镇下");
        }

        // 明细必填
        if ("1".equals(dto.getDeclarationCategory()) &&
                (dto.getHouseholdIds() == null || dto.getHouseholdIds().isEmpty())) {
            throw new BizException("到户项目必须至少勾选1户农户");
        }
        if ("2".equals(dto.getDeclarationCategory()) &&
                (dto.getFacilityIds() == null || dto.getFacilityIds().isEmpty())) {
            throw new BizException("公建项目必须至少勾选1个公共基础设施");
        }

        // 更新主表（业务编号、乡镇、类别不可改）
        if (dto.getDeclarationDate() != null) {
            exist.setDeclarationDate(dto.getDeclarationDate());
        }
        exist.setTitle(trimToNull(dto.getTitle()));
        exist.setVillageGroupCode(vgRegion.getCode());
        exist.setVillageGroupName(vgRegion.getName());
        exist.setDeclarationType(trimToNull(dto.getDeclarationType()));
        exist.setAmount(dto.getAmount());
        exist.setRemark(trimToNull(dto.getRemark()));
        exist.setUpdateBy(currentUser());
        exist.setUpdateTime(LocalDateTime.now());
        declarationMapper.updateById(exist);

        // 明细：先逻辑删，再批量插（快照刷新为最新）
        householdMapper.logicDeleteByDeclarationId(exist.getId());
        facilityMapper.logicDeleteByDeclarationId(exist.getId());
        saveHouseholdSnapshots(exist.getId(), dto.getHouseholdIds(), vgRegion);
        saveFacilitySnapshots(exist.getId(), dto.getFacilityIds(), vgRegion);

        // 附件：差异保存
        saveAttachments("declaration", exist.getId(), dto.getAttachments(), exist.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Declaration exist = declarationMapper.selectById(id);
        if (exist == null) {
            throw new BizException("申报单不存在或已被删除");
        }
        // 逻辑删除主表（MyBatis-Plus 自动处理 @TableLogic）
        declarationMapper.deleteById(id);
        // 明细+附件 一并清理，保持数据一致性
        householdMapper.logicDeleteByDeclarationId(id);
        facilityMapper.logicDeleteByDeclarationId(id);
        attachmentMapper.logicDeleteByOwner("declaration", id);
        // 申报单级附件是 owner_type=declaration 的，上面已经删过了；明细级附件保留（它们属于档案级）
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(DeclarationSubmitDTO dto) {
        Declaration exist = declarationMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("申报单不存在或已被删除");
        }
        // 仅已通过的申报单不允许再次提交（驳回的允许重提，重提时 rejected 明细会被 reset 为 pending）
        if ("approved".equals(exist.getStatus())) {
            throw new BizException("已通过的申报单不允许再次提交");
        }
        // 校验接收审核人存在且启用
        SysUser recipient = userMapper.selectById(dto.getRecipientUserId());
        if (recipient == null) {
            throw new BizException("接收审核人不存在");
        }
        if (recipient.getDeleted() != null && recipient.getDeleted() == 1) {
            throw new BizException("接收审核人已被删除");
        }
        if (recipient.getStatus() != null && recipient.getStatus() != 1) {
            throw new BizException("接收审核人已停用，请选择启用中的用户");
        }
        Declaration update = new Declaration();
        update.setId(dto.getId());
        update.setStatus("submitted");
        update.setRecipientUserId(recipient.getId());
        update.setUpdateBy(currentUser());
        update.setUpdateTime(LocalDateTime.now());
        declarationMapper.updateById(update);

        // 提交/重提：未提交 + 被驳回的明细 reset 为 pending；已通过的不动
        LoginUser me2 = UserContext.get();
        String submitterName = me2 == null ? null : (me2.getRealName() == null ? me2.getUsername() : me2.getRealName());

        // 提交/重提：日志 action 按 beforeAuditStatus 区分：
        //   before = unsubmitted → SUBMIT_FIRST          首次提交审核
        //   before = rejected   → SUBMIT_RESUBMITTED    重新提交审核（驳回后重提）
        List<DeclarationHousehold> pendingHouseholds = householdMapper.selectList(
                new LambdaQueryWrapper<DeclarationHousehold>()
                        .eq(DeclarationHousehold::getDeclarationId, exist.getId())
                        .in(DeclarationHousehold::getAuditStatus, "unsubmitted", "rejected"));
        for (DeclarationHousehold hh : pendingHouseholds) {
            String before = hh.getAuditStatus();
            hh.setAuditStatus("pending");
            householdMapper.updateById(hh);
            String action = "unsubmitted".equals(before) ? "SUBMIT_FIRST" : "SUBMIT_RESUBMITTED";
            writeAuditLog(action, exist.getId(), "household", hh.getId(),
                    hh.getSnapshotHeadName(), before, "pending",
                    null, me2 == null ? null : me2.getUserId(), submitterName);
        }

        List<DeclarationPublicFacility> pendingFacilities = facilityMapper.selectList(
                new LambdaQueryWrapper<DeclarationPublicFacility>()
                        .eq(DeclarationPublicFacility::getDeclarationId, exist.getId())
                        .in(DeclarationPublicFacility::getAuditStatus, "unsubmitted", "rejected"));
        for (DeclarationPublicFacility pf : pendingFacilities) {
            String before = pf.getAuditStatus();
            pf.setAuditStatus("pending");
            facilityMapper.updateById(pf);
            String action = "unsubmitted".equals(before) ? "SUBMIT_FIRST" : "SUBMIT_RESUBMITTED";
            writeAuditLog(action, exist.getId(), "public_facility", pf.getId(),
                    pf.getSnapshotFacilityName(), before, "pending",
                    null, me2 == null ? null : me2.getUserId(), submitterName);
        }
    }

    @Override
    public PageVO<DeclarationDetailVO> receivedPage(long pageNum, long pageSize, String title,
                                                     String declarationType, String declarationCategory,
                                                     String status) {
        LoginUser me = UserContext.get();
        if (me == null || me.getUserId() == null) {
            throw new BizException("登录已过期");
        }
        // tab status → auditStatus 匹配集合（审核人端 submitted 只匹配 pending，unsubmitted 是申报人还没点提交）
        // 注意：approved 只放 auditStatus=approved（已立项 established 归已立项 tab），tab 语义必须严格互斥
        List<String> targetAuditStatuses = null;
        if ("submitted".equals(status)) targetAuditStatuses = Collections.singletonList("pending");
        else if ("approved".equals(status)) targetAuditStatuses = Collections.singletonList("approved");
        else if ("rejected".equals(status)) targetAuditStatuses = Collections.singletonList("rejected");
        // 已立项：established 是明细级状态（父单 status 仍为 approved），必须走明细 audit_status 匹配，
        // 不能落到下面 targetAuditStatuses==null 的父单 status='established' 分支（父单无此状态会查空）
        else if ("established".equals(status)) targetAuditStatuses = Collections.singletonList("established");

        Page<Declaration> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Declaration> wrapper = new LambdaQueryWrapper<Declaration>()
                .eq(Declaration::getRecipientUserId, me.getUserId())
                .like(isNotBlank(title), Declaration::getTitle, isNotBlank(title) ? title.trim() : null)
                .eq(isNotBlank(declarationType), Declaration::getDeclarationType, declarationType)
                .eq(isNotBlank(declarationCategory), Declaration::getDeclarationCategory, declarationCategory)
                .eq(targetAuditStatuses == null && isNotBlank(status), Declaration::getStatus, status);
        // tab 分类逻辑（audit_status 匹配）：搬到 Java 层，统一走 collectDeclIdsByAuditStatus 工具方法
        if (targetAuditStatuses != null) {
            Set<Long> matchingDeclIds = collectDeclIdsByAuditStatus(targetAuditStatuses);
            if (matchingDeclIds.isEmpty()) matchingDeclIds.add(-1L);
            wrapper.in(Declaration::getId, matchingDeclIds);
        }
        wrapper.orderByDesc(Declaration::getCreateTime);
        Page<Declaration> result = declarationMapper.selectPage(page, wrapper);
        List<DeclarationDetailVO> records = new ArrayList<>();
        // 用同一个 targetAuditStatuses 过滤子明细，避免 mixed 场景下脏明细穿透到本 tab
        for (Declaration d : result.getRecords()) {
            records.add(buildListVO(d, targetAuditStatuses));
        }
        PageVO<DeclarationDetailVO> vo = new PageVO<>();
        vo.setRecords(records);
        vo.setTotal(result.getTotal());
        vo.setPageNum(pageNum);
        vo.setPageSize(pageSize);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(DeclarationAuditDTO dto) {
        Declaration exist = declarationMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("申报单不存在或已被删除");
        }
        if (!"submitted".equals(exist.getStatus())) {
            throw new BizException("只有待审核状态的申报单可以审核，当前状态：" + exist.getStatus());
        }
        // 校验当前登录人是被指派的审核人
        LoginUser me = UserContext.get();
        if (me == null || me.getUserId() == null) {
            throw new BizException("登录已过期");
        }
        if (exist.getRecipientUserId() == null || !exist.getRecipientUserId().equals(me.getUserId())) {
            throw new BizException("该申报单未指派给您审核，无权操作");
        }
        if ("reject".equals(dto.getAction())
                && (dto.getAuditRemark() == null || dto.getAuditRemark().isBlank())) {
            throw new BizException("驳回必须填写审核意见");
        }
        Declaration update = new Declaration();
        update.setId(dto.getId());
        update.setStatus("approve".equals(dto.getAction()) ? "approved" : "rejected");
        update.setAuditBy(me.getUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(trimToNull(dto.getAuditRemark()));
        update.setUpdateBy(currentUser());
        update.setUpdateTime(LocalDateTime.now());
        declarationMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditDetail(DeclarationDetailAuditDTO dto) {
        // 1. 明细存在性 + 归属申报单校验
        Declaration parent;
        Long declarationId;
        LoginUser me = UserContext.get();
        if (me == null || me.getUserId() == null) {
            throw new BizException("登录已过期");
        }
        if ("household".equals(dto.getDetailKind())) {
            DeclarationHousehold hh = householdMapper.selectById(dto.getDetailId());
            if (hh == null) {
                throw new BizException("农户明细不存在或已删除");
            }
            declarationId = hh.getDeclarationId();
            // 先存 beforeStatus（改 hh 会覆盖）
            String beforeStatus = hh.getAuditStatus();
            hh.setAuditStatus("approve".equals(dto.getAction()) ? "approved"
                              : "reject".equals(dto.getAction()) ? "rejected"
                              : "revoke".equals(dto.getAction()) ? "pending" : null);
            if (hh.getAuditStatus() == null) {
                throw new BizException("未知审核动作：" + dto.getAction());
            }
            hh.setAuditBy(me.getUserId());
            hh.setAuditTime(LocalDateTime.now());
            hh.setAuditRemark(trimToNull(dto.getAuditRemark()));
            householdMapper.updateById(hh);

            // 写审核历史日志（updateById 之后，hh.auditStatus 已是新值 = afterStatus）
            writeAuditLog(
                    auditLogAction(dto.getAction()),
                    declarationId, "household", hh.getId(),
                    hh.getSnapshotHeadName(),
                    beforeStatus, hh.getAuditStatus(),
                    dto.getAuditRemark(),
                    me.getUserId(), me.getRealName() == null ? me.getUsername() : me.getRealName());
        } else {
            DeclarationPublicFacility pf = facilityMapper.selectById(dto.getDetailId());
            if (pf == null) {
                throw new BizException("公建设施明细不存在或已删除");
            }
            declarationId = pf.getDeclarationId();
            String beforeStatus = pf.getAuditStatus();
            pf.setAuditStatus("approve".equals(dto.getAction()) ? "approved"
                              : "reject".equals(dto.getAction()) ? "rejected"
                              : "revoke".equals(dto.getAction()) ? "pending" : null);
            if (pf.getAuditStatus() == null) {
                throw new BizException("未知审核动作：" + dto.getAction());
            }
            pf.setAuditBy(me.getUserId());
            pf.setAuditTime(LocalDateTime.now());
            pf.setAuditRemark(trimToNull(dto.getAuditRemark()));
            facilityMapper.updateById(pf);

            writeAuditLog(
                    auditLogAction(dto.getAction()),
                    declarationId, "public_facility", pf.getId(),
                    pf.getSnapshotFacilityName(),
                    beforeStatus, pf.getAuditStatus(),
                    dto.getAuditRemark(),
                    me.getUserId(), me.getRealName() == null ? me.getUsername() : me.getRealName());
        }
        parent = declarationMapper.selectById(declarationId);
        if (parent == null) {
            throw new BizException("所属申报单不存在");
        }

        // 2. 父申报单权限校验 + 动作约束
        // approve/reject：只能对 pending 明细做，父表必须 submitted
        // revoke：只能对 approved/rejected 明细做，父表必须 submitted
        if (!"submitted".equals(parent.getStatus())) {
            throw new BizException("申报单当前状态不允许明细审核：" + parent.getStatus());
        }
        if (parent.getRecipientUserId() == null || !parent.getRecipientUserId().equals(me.getUserId())) {
            throw new BizException("该申报单未指派给您审核，无权操作其明细");
        }
        boolean needRemark = "reject".equals(dto.getAction()) || "revoke".equals(dto.getAction());
        if (needRemark && (dto.getAuditRemark() == null || dto.getAuditRemark().isBlank())) {
            throw new BizException(("reject".equals(dto.getAction()) ? "驳回" : "撤销") + "必须填写审核意见");
        }

        // 3. 自动推进父申报单状态
        autoPromoteParentStatus(parent, me.getUserId(), trimToNull(dto.getAuditRemark()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revertDetail(DeclarationDetailRevertDTO dto) {
        LoginUser me = UserContext.get();
        if (me == null || me.getUserId() == null) {
            throw new BizException("登录已过期");
        }
        Long declarationId;
        String auditRemark = trimToNull(dto.getRemark());
        // 1. 撤销该条明细的审核结论：audit_status 置回 pending，并清空审核痕迹。
        //    必须用 LambdaUpdateWrapper.set(field, null) 显式置空——MyBatis-Plus updateById 默认
        //    NOT_NULL 策略会跳过 null 字段，导致 audit_by / audit_time 清不掉。
        if ("household".equals(dto.getDetailKind())) {
            DeclarationHousehold hh = householdMapper.selectById(dto.getDetailId());
            if (hh == null) throw new BizException("农户明细不存在或已删除");
            declarationId = hh.getDeclarationId();
            String beforeStatus = hh.getAuditStatus();
            householdMapper.update(null, new LambdaUpdateWrapper<DeclarationHousehold>()
                    .eq(DeclarationHousehold::getId, hh.getId())
                    .set(DeclarationHousehold::getAuditStatus, "pending")
                    .set(DeclarationHousehold::getAuditBy, null)
                    .set(DeclarationHousehold::getAuditTime, null)
                    .set(DeclarationHousehold::getAuditRemark, auditRemark));   // 撤销备注留痕，重新审核时会被覆盖

            // 写审核撤销日志（afterStatus 固定 pending，beforeStatus 就是 LambdaUpdateWrapper 之前的值）
            writeAuditLog("AUDIT_REVOKE", declarationId, "household", hh.getId(),
                    hh.getSnapshotHeadName(), beforeStatus, "pending",
                    auditRemark, me.getUserId(),
                    me.getRealName() == null ? me.getUsername() : me.getRealName());
        } else {
            DeclarationPublicFacility pf = facilityMapper.selectById(dto.getDetailId());
            if (pf == null) throw new BizException("公建设施明细不存在或已删除");
            declarationId = pf.getDeclarationId();
            String beforeStatus = pf.getAuditStatus();
            facilityMapper.update(null, new LambdaUpdateWrapper<DeclarationPublicFacility>()
                    .eq(DeclarationPublicFacility::getId, pf.getId())
                    .set(DeclarationPublicFacility::getAuditStatus, "pending")
                    .set(DeclarationPublicFacility::getAuditBy, null)
                    .set(DeclarationPublicFacility::getAuditTime, null)
                    .set(DeclarationPublicFacility::getAuditRemark, auditRemark));

            writeAuditLog("AUDIT_REVOKE", declarationId, "public_facility", pf.getId(),
                    pf.getSnapshotFacilityName(), beforeStatus, "pending",
                    auditRemark, me.getUserId(),
                    me.getRealName() == null ? me.getUsername() : me.getRealName());
        }

        // 2. 父申报单存在性校验
        Declaration parent = declarationMapper.selectById(declarationId);
        if (parent == null) throw new BizException("所属申报单不存在");
        // 注：不再强校验 recipient_user_id 必须等于当前登录人——"已通过/已驳回"页签里
        // 的单本身已经在 receivedPage 里按 recipient_user_id = me.getUserId() 过滤过了，
        // 二次校验是多余的；同时也方便管理员兜底撤销。

        // 3. 撤销审核结论：父申报单回到 submitted（待审核），【保留】recipient_user_id 审核指派，
        //    仅清空父单上一次的审核结论字段。这样该单会同时回到：
        //      ① 申报审核-待审核页签：审核人可重新做 通过/驳回；
        //      ② 申报管理-待审核页签：明细显示"待审核"，且因处于审核中（submitted）申报人不可编辑、不可重复提交。
        declarationMapper.update(null, new LambdaUpdateWrapper<Declaration>()
                .eq(Declaration::getId, parent.getId())
                .set(Declaration::getStatus, "submitted")
                .set(Declaration::getAuditBy, null)
                .set(Declaration::getAuditTime, null)
                .set(Declaration::getAuditRemark, auditRemark != null ? ("明细撤销：" + auditRemark) : "明细撤销，回到待审核")
                .set(Declaration::getUpdateBy, currentUser())
                .set(Declaration::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 明细审核后自动汇总父申报单状态：
     *   - 所有明细 approved  → 父 approved
     *   - 有 rejected 且无 pending → 父 rejected（混合 approved/rejected，视为整体驳回）
     *   - 其余（仍有 pending）→ 父保持 submitted
     */
    private void autoPromoteParentStatus(Declaration parent, Long operatorId, String remark) {
        String category = parent.getDeclarationCategory();
        List<String> statuses;
        if ("1".equals(category)) {
            statuses = householdMapper.selectList(new LambdaQueryWrapper<DeclarationHousehold>()
                            .eq(DeclarationHousehold::getDeclarationId, parent.getId()))
                    .stream()
                    .map(DeclarationHousehold::getAuditStatus)
                    .toList();
        } else if ("2".equals(category)) {
            statuses = facilityMapper.selectList(new LambdaQueryWrapper<DeclarationPublicFacility>()
                            .eq(DeclarationPublicFacility::getDeclarationId, parent.getId()))
                    .stream()
                    .map(DeclarationPublicFacility::getAuditStatus)
                    .toList();
        } else {
            return;
        }
        if (statuses.isEmpty()) {
            return;
        }
        boolean allApproved = statuses.stream().allMatch(s -> "approved".equals(s) || "established".equals(s));
        boolean anyRejected = statuses.stream().anyMatch("rejected"::equals);
        boolean anyPending = statuses.stream().anyMatch(s -> s == null || s.isEmpty() || "pending".equals(s));
        // established 明细是"已通过"的升级状态，不算 pending；但如果有 pending 则整体仍未完成

        Declaration update = new Declaration();
        update.setId(parent.getId());
        update.setUpdateBy(currentUser());
        update.setUpdateTime(LocalDateTime.now());
        if (allApproved) {
            update.setStatus("approved");
            update.setAuditBy(operatorId);
            update.setAuditTime(LocalDateTime.now());
            update.setAuditRemark("明细全部审核通过");
            declarationMapper.updateById(update);
        } else if (anyRejected && !anyPending) {
            update.setStatus("rejected");
            update.setAuditBy(operatorId);
            update.setAuditTime(LocalDateTime.now());
            update.setAuditRemark(remark != null ? remark : "明细全部审核完毕，存在驳回项");
            declarationMapper.updateById(update);
        }
        // 仍有 pending → 不推进，父保持 submitted
    }

    @Override
    public List<Map<String, Object>> listRecipients(String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .and(isNotBlank(keyword), w -> w
                        .like(SysUser::getRealName, keyword)
                        .or()
                        .like(SysUser::getUsername, keyword))
                .orderByAsc(SysUser::getRealName)
                .last("LIMIT 100");  // 避免下拉太多
        List<SysUser> list = userMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查区划名称做冗余（前端直接显示）
        Set<String> regionCodes = list.stream()
                .map(SysUser::getRegionCode).filter(this::isNotBlank).collect(Collectors.toSet());
        Map<String, String> regionNameMap = new HashMap<>();
        if (!regionCodes.isEmpty()) {
            List<SysRegion> regions = regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                    .in(SysRegion::getCode, regionCodes));
            for (SysRegion r : regions) {
                regionNameMap.put(r.getCode(), r.getName());
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysUser u : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("phone", u.getPhone());
            m.put("regionCode", u.getRegionCode());
            m.put("regionName", regionNameMap.getOrDefault(u.getRegionCode(), ""));
            out.add(m);
        }
        return out;
    }

    /**
     * 根据 tab 对应的明细 audit_status 集合，查出所有含有至少一条匹配明细的 declaration_id 集合。
     * 统一在 Java 层做 EXISTS，绕开 MP .apply() 占位符不可靠的坑。
     */
    private Set<Long> collectDeclIdsByAuditStatus(List<String> targetAuditStatuses) {
        Set<Long> result = new HashSet<>();
        // household 明细：把 IN(...) OR audit_status IS NULL 包在同一子分组里，
        // 避免全局 OR 把 deleted=0 之外的条件切断导致匹配扩散。
        LambdaQueryWrapper<DeclarationHousehold> hw = new LambdaQueryWrapper<>();
        hw.select(DeclarationHousehold::getDeclarationId)
          .and(w -> w
              .inSql(DeclarationHousehold::getAuditStatus, buildInExpr(targetAuditStatuses))
              .or(h -> h.isNull(DeclarationHousehold::getAuditStatus)))
          .eq(DeclarationHousehold::getDeleted, 0);
        for (DeclarationHousehold h : householdMapper.selectList(hw)) {
            if (h.getDeclarationId() != null) result.add(h.getDeclarationId());
        }
        // public_facility 明细：同样用子分组包住 IN(...) OR audit_status IS NULL
        LambdaQueryWrapper<DeclarationPublicFacility> fw = new LambdaQueryWrapper<>();
        fw.select(DeclarationPublicFacility::getDeclarationId)
          .and(w -> w
              .inSql(DeclarationPublicFacility::getAuditStatus, buildInExpr(targetAuditStatuses))
              .or(f -> f.isNull(DeclarationPublicFacility::getAuditStatus)))
          .eq(DeclarationPublicFacility::getDeleted, 0);
        for (DeclarationPublicFacility f : facilityMapper.selectList(fw)) {
            if (f.getDeclarationId() != null) result.add(f.getDeclarationId());
        }
        return result;
    }

    /** 把字符串列表拼成 SQL IN 子句（带引号），用于 inSql() —— 仅限内部可信数据，不会有注入风险 */
    private String buildInExpr(List<String> values) {
        return values.stream().map(v -> "'" + v + "'").collect(Collectors.joining(","));
    }

    /** 列表场景：把主表 + 明细 + 冗余审核人姓名合成一个 VO（submit/received page 共用） */
    private DeclarationDetailVO buildListVO(Declaration d, List<String> targetAuditStatuses) {
        DeclarationDetailVO vo = toSimpleVO(d);
        // 冗余审核人姓名
        if (d.getRecipientUserId() != null) {
            SysUser r = userMapper.selectById(d.getRecipientUserId());
            vo.setRecipientRealName(r != null ? r.getRealName() : null);
        }
        if (d.getAuditBy() != null) {
            SysUser a = userMapper.selectById(d.getAuditBy());
            vo.setAuditByRealName(a != null ? a.getRealName() : null);
        }
        // 列表树形：一次性查明细（有 targetAuditStatuses 时 IN 过滤出对应状态子集）
        if ("1".equals(d.getDeclarationCategory())) {
            List<DeclarationHousehold> hhList = householdMapper.selectList(
                    new LambdaQueryWrapper<DeclarationHousehold>()
                            .eq(DeclarationHousehold::getDeclarationId, d.getId())
                            .in(targetAuditStatuses != null, DeclarationHousehold::getAuditStatus, targetAuditStatuses)
                            .orderByAsc(DeclarationHousehold::getId));
            List<DeclarationDetailVO.HouseholdVO> hVOs = hhList.stream().map(hh -> {
                DeclarationDetailVO.HouseholdVO hvo = new DeclarationDetailVO.HouseholdVO();
                hvo.setId(hh.getId());
                hvo.setHouseholdId(hh.getHouseholdId());
                hvo.setSnapshotHouseholdNo(hh.getSnapshotHouseholdNo());
                hvo.setSnapshotHeadName(hh.getSnapshotHeadName());
                hvo.setSnapshotHouseholdNo(hh.getSnapshotHouseholdNo());
                hvo.setSnapshotHeadIdCard(hh.getSnapshotHeadIdCard());
                hvo.setSnapshotContactPhone(hh.getSnapshotContactPhone());
                hvo.setSnapshotHouseholdType(hh.getSnapshotHouseholdType());
                hvo.setSnapshotRegionName(hh.getSnapshotRegionName());
                hvo.setAmount(hh.getAmount());
                hvo.setRemark(hh.getRemark());
                hvo.setAuditStatus(hh.getAuditStatus() == null ? "unsubmitted" : hh.getAuditStatus());
                hvo.setAuditBy(hh.getAuditBy());
                hvo.setAuditTime(hh.getAuditTime() == null ? null : hh.getAuditTime().toString());
                hvo.setAuditRemark(hh.getAuditRemark());
                hvo.setAttachments(listAttachments("household", hh.getId()));
                return hvo;
            }).toList();
            fillHouseholdAuditByName(hVOs);
            vo.setHouseholds(hVOs);
        } else if ("2".equals(d.getDeclarationCategory())) {
            List<DeclarationPublicFacility> pfList = facilityMapper.selectList(
                    new LambdaQueryWrapper<DeclarationPublicFacility>()
                            .eq(DeclarationPublicFacility::getDeclarationId, d.getId())
                            .in(targetAuditStatuses != null, DeclarationPublicFacility::getAuditStatus, targetAuditStatuses)
                            .orderByAsc(DeclarationPublicFacility::getId));
            List<DeclarationDetailVO.FacilityVO> fVOs = pfList.stream().map(pf -> {
                DeclarationDetailVO.FacilityVO fvo = new DeclarationDetailVO.FacilityVO();
                fvo.setId(pf.getId());
                fvo.setFacilityId(pf.getFacilityId());
                fvo.setSnapshotFacilityCode(pf.getSnapshotFacilityCode());
                fvo.setSnapshotFacilityName(pf.getSnapshotFacilityName());
                fvo.setSnapshotFacilityType(pf.getSnapshotFacilityType());
                fvo.setSnapshotLocation(pf.getSnapshotLocation());
                fvo.setSnapshotRegionName(pf.getSnapshotRegionName());
                fvo.setAmount(pf.getAmount());
                fvo.setRemark(pf.getRemark());
                fvo.setAuditStatus(pf.getAuditStatus() == null ? "unsubmitted" : pf.getAuditStatus());
                fvo.setAuditBy(pf.getAuditBy());
                fvo.setAuditTime(pf.getAuditTime() == null ? null : pf.getAuditTime().toString());
                fvo.setAuditRemark(pf.getAuditRemark());
                // 明细级附件：owner_type=public_facility, owner_id=明细ID
                fvo.setAttachments(listAttachments("public_facility", pf.getId()));
                return fvo;
            }).toList();
            fillFacilityAuditByName(fVOs);
            vo.setFacilities(fVOs);
        }
        return vo;
    }

    @Override
    public UserRegionVO resolveUserTown() {
        LoginUser loginUser = UserContext.get();
        String startCode = loginUser == null ? null : loginUser.getRegionCode();
        UserRegionVO vo = new UserRegionVO();
        if (!isNotBlank(startCode)) {
            // 管理员未绑定区划，返回 null 让前端提示
            vo.setCurrentRegionCode(null);
            vo.setCurrentRegionName(null);
            vo.setTownCode(null);
            vo.setTownName(null);
            return vo;
        }
        // 向上递归（最多 6 层：省-市-区县-乡镇-村-组）
        String currentCode = startCode.trim();
        SysRegion currentRegion = null;
        for (int i = 0; i < 6; i++) {
            SysRegion r = regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                    .eq(SysRegion::getCode, currentCode));
            if (r == null) {
                break;
            }
            // 第一次查到的就是当前区划
            if (i == 0) {
                vo.setCurrentRegionCode(r.getCode());
                vo.setCurrentRegionName(r.getName());
                vo.setCurrentRegionType(r.getRegionType());
                vo.setCurrentRegionLevel(r.getLevel());
                currentRegion = r;
            }
            // 命中乡镇 → 回填（按区划类型=乡镇优先，level==4 兜底，双保险）
            //   regionType='1' —— 字典 qu_hua_lei_xing 的乡镇值
            //   level=4 —— 系统内置层级 REGION_LEVEL_TOWN
            boolean isTownRegion = ("1".equals(r.getRegionType())
                    || (r.getLevel() != null && r.getLevel() == Constants.REGION_LEVEL_TOWN));
            if (isTownRegion) {
                vo.setTownCode(r.getCode());
                vo.setTownName(r.getName());
                return vo;
            }
            // 没找到乡镇但也没有上级了，退出循环
            if (!isNotBlank(r.getParentCode())) {
                break;
            }
            currentCode = r.getParentCode();
        }
        // 找不到乡镇
        vo.setTownCode(null);
        vo.setTownName(null);
        return vo;
    }

    @Override
    public List<Map<String, Object>> listVillageGroupsByTown(String townCode) {
        if (!isNotBlank(townCode)) {
            return Collections.emptyList();
        }
        // 乡镇下的村/组（level >= 5 且 parentCode 以 townCode 开头）
        List<SysRegion> list = regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getParentCode, townCode.trim())
                .orderByAsc(SysRegion::getSort));
        return list.stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("code", r.getCode());
            item.put("name", r.getName());
            item.put("level", r.getLevel());
            item.put("regionType", r.getRegionType());
            return item;
        }).toList();
    }

    @Override
    public List<Map<String, Object>> listHouseholdsByVillageGroup(String villageGroupCode) {
        if (!isNotBlank(villageGroupCode)) {
            return Collections.emptyList();
        }
        // 递归收集所选村组 + 下设所有子级的区划编码（村选了要覆盖村组；村分组选了覆盖组）
        Set<String> scopeCodes = collectAllDescendantCodes(villageGroupCode.trim());
        List<Household> list = baseHouseholdMapper.selectList(new LambdaQueryWrapper<Household>()
                .in(Household::getRegionCode, scopeCodes)
                .orderByDesc(Household::getCreateTime));
        return list.stream().map(h -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", String.valueOf(h.getId()));
            item.put("householdNo", h.getHouseholdNo());
            item.put("headName", h.getHeadName());
            item.put("contactPhone", h.getContactPhone());
            item.put("householdType", h.getHouseholdType());
            item.put("householdTypeName", dictLabelOf("hu_lei_xing", h.getHouseholdType()));
            return item;
        }).toList();
    }

    @Override
    public List<Map<String, Object>> listFacilitiesByVillageGroup(String villageGroupCode) {
        if (!isNotBlank(villageGroupCode)) {
            return Collections.emptyList();
        }
        Set<String> scopeCodes = collectAllDescendantCodes(villageGroupCode.trim());
        List<BaseFacility> list = baseFacilityMapper.selectList(new LambdaQueryWrapper<BaseFacility>()
                .in(BaseFacility::getRegionCode, scopeCodes)
                .orderByDesc(BaseFacility::getCreateTime));
        return list.stream().map(f -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", String.valueOf(f.getId()));
            item.put("facilityCode", f.getFacilityCode());
            item.put("facilityName", f.getFacilityName());
            item.put("facilityType", f.getFacilityType());
            item.put("facilityTypeName", dictLabelOf("she_shi_lei_xing", f.getFacilityType()));
            item.put("location", f.getLocation());
            return item;
        }).toList();
    }

    // ================== 内部辅助 ==================

    /**
     * 业务编号生成：DEC + yyyyMMdd + 4位自增序号
     * 并发下可能重复（理论上雪花ID主键冲突概率极低，但业务编号是唯一约束），
     * 实际通过"查当天最大编号+1"生成，如遇冲突重试（最多3次）
     */
    @Override
    public String generateBusinessCode() {
        String date = LocalDate.now().format(CODE_DATE_FMT);
        for (int attempt = 0; attempt < 3; attempt++) {
            String prefix = CODE_PREFIX + date;
            Declaration max = declarationMapper.selectOne(new LambdaQueryWrapper<Declaration>()
                    .likeRight(Declaration::getBusinessCode, prefix)
                    .orderByDesc(Declaration::getBusinessCode)
                    .last("LIMIT 1"));
            int seq = 1;
            if (max != null && max.getBusinessCode() != null && max.getBusinessCode().length() == prefix.length() + CODE_SEQ_LEN) {
                String seqStr = max.getBusinessCode().substring(prefix.length());
                try {
                    seq = Integer.parseInt(seqStr) + 1;
                } catch (NumberFormatException ignore) { /* 跳过 */ }
            }
            String code = prefix + String.format("%0" + CODE_SEQ_LEN + "d", seq);
            if (declarationMapper.countByBusinessCode(code) == 0) {
                return code;
            }
        }
        return CODE_PREFIX + date + System.currentTimeMillis() % 100000;
    }

    @Override
    public boolean isBusinessCodeUnique(String businessCode) {
        if (businessCode == null || businessCode.isBlank()) return true;
        return declarationMapper.countByBusinessCode(businessCode.trim()) == 0;
    }

    /** 保存农户快照：DTO householdIds 只传 ID，后端查最新档案数据写快照 */
    private void saveHouseholdSnapshots(Long declarationId, List<Long> householdIds, SysRegion expectedVg) {
        if (householdIds == null || householdIds.isEmpty()) {
            return;
        }
        // 查农户档案最新数据
        List<Household> hs = baseHouseholdMapper.selectBatchIds(householdIds);
        Set<String> allowedCodes = resolveAllowedVgCodes(expectedVg);
        for (Household h : hs) {
            if (h == null) {
                continue;
            }
            // 安全检查：只允许选当前村组下的农户（防止篡改ID）
            if (!allowedCodes.contains(h.getRegionCode())) {
                continue;
            }
            DeclarationHousehold dhh = new DeclarationHousehold();
            dhh.setDeclarationId(declarationId);
            dhh.setHouseholdId(h.getId());
            dhh.setSnapshotHouseholdNo(h.getHouseholdNo());
            dhh.setSnapshotHeadName(h.getHeadName());
            dhh.setSnapshotHouseholdNo(h.getHouseholdNo());
            dhh.setSnapshotHeadIdCard(h.getHeadIdCard());
            dhh.setSnapshotContactPhone(h.getContactPhone());
            dhh.setSnapshotHouseholdType(h.getHouseholdType());
            dhh.setSnapshotRegionCode(h.getRegionCode());
            dhh.setSnapshotRegionName(h.getRegionName());
            dhh.setAuditStatus("unsubmitted");  // 新建时明细状态=未提交
            dhh.setCreateTime(LocalDateTime.now());
            householdMapper.insert(dhh);
        }
    }

    /** 保存公建设施快照 */
    private void saveFacilitySnapshots(Long declarationId, List<Long> facilityIds, SysRegion expectedVg) {
        if (facilityIds == null || facilityIds.isEmpty()) {
            return;
        }
        List<BaseFacility> fs = baseFacilityMapper.selectBatchIds(facilityIds);
        Set<String> allowedCodes = resolveAllowedVgCodes(expectedVg);
        for (BaseFacility f : fs) {
            if (f == null) {
                continue;
            }
            if (!allowedCodes.contains(f.getRegionCode())) {
                continue;
            }
            DeclarationPublicFacility dpf = new DeclarationPublicFacility();
            dpf.setDeclarationId(declarationId);
            dpf.setFacilityId(f.getId());
            dpf.setSnapshotFacilityCode(f.getFacilityCode());
            dpf.setSnapshotFacilityName(f.getFacilityName());
            dpf.setSnapshotFacilityType(f.getFacilityType());
            dpf.setSnapshotLocation(f.getLocation());
            dpf.setSnapshotRegionCode(f.getRegionCode());
            dpf.setSnapshotRegionName(f.getRegionName());
            dpf.setAuditStatus("unsubmitted");  // 新建时明细状态=未提交
            dpf.setCreateTime(LocalDateTime.now());
            facilityMapper.insert(dpf);
        }
    }

    /**
     * 给定一个村组/行政村区划，收集其自身编码 + 所有下级组编码
     * 安全检查用：确保农户/设施归属区划在用户选择的村组范围内
     */
    private Set<String> resolveAllowedVgCodes(SysRegion vgRegion) {
        Set<String> codes = new java.util.HashSet<>();
        if (vgRegion == null) {
            return codes;
        }
        codes.add(vgRegion.getCode());
        // 递归收集 level >= 5 的下级（村/组）
        collectChildren(vgRegion.getCode(), codes);
        return codes;
    }

    private void collectChildren(String parentCode, Set<String> sink) {
        List<SysRegion> children = regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getParentCode, parentCode));
        for (SysRegion c : children) {
            sink.add(c.getCode());
            collectChildren(c.getCode(), sink);
        }
    }

    /** 保存附件：新增时全量插；编辑时按 owner 做差异对比 */
    private void saveAttachments(String ownerType, Long ownerId,
                                 List<DeclarationSaveDTO.AttachmentItem> attachments,
                                 Long forCompareOwnerId) {
        if (attachments == null || attachments.isEmpty()) {
            if (forCompareOwnerId == null) {
                return; // 新增场景无附件就跳过
            }
            return;
        }
        // 编辑场景：对比已有附件
        if (forCompareOwnerId != null) {
            List<DeclarationAttachment> existList = attachmentMapper.selectList(
                    new LambdaQueryWrapper<DeclarationAttachment>()
                            .eq(DeclarationAttachment::getOwnerType, ownerType)
                            .eq(DeclarationAttachment::getOwnerId, ownerId));
            Set<Long> inputIds = attachments.stream()
                    .map(DeclarationSaveDTO.AttachmentItem::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            for (DeclarationAttachment att : existList) {
                if (!inputIds.contains(att.getId())) {
                    attachmentMapper.deleteById(att.getId());
                }
            }
        }
        for (DeclarationSaveDTO.AttachmentItem item : attachments) {
            if (item.getId() != null && forCompareOwnerId != null) {
                DeclarationAttachment att = attachmentMapper.selectById(item.getId());
                if (att != null) {
                    att.setAttachmentType(trimToNull(item.getAttachmentType()));
                    att.setFileName(trimToNull(item.getFileName()));
                    att.setFileKey(trimToNull(item.getFileKey()));
                    att.setBucket(trimToNull(item.getBucket()));
                    att.setFileSize(item.getFileSize());
                    att.setRemark(trimToNull(item.getRemark()));
                    attachmentMapper.updateById(att);
                    continue;
                }
            }
            DeclarationAttachment att = new DeclarationAttachment();
            att.setOwnerType(ownerType);
            att.setOwnerId(ownerId);
            att.setAttachmentType(trimToNull(item.getAttachmentType()));
            att.setFileName(trimToNull(item.getFileName()));
            att.setFileKey(trimToNull(item.getFileKey()));
            att.setBucket(trimToNull(item.getBucket()));
            att.setFileSize(item.getFileSize());
            att.setRemark(trimToNull(item.getRemark()));
            att.setCreateBy(currentUser());
            att.setCreateTime(LocalDateTime.now());
            attachmentMapper.insert(att);
        }
    }

    /** 按 owner 查附件列表，VO 映射 */
    private List<DeclarationDetailVO.AttachmentVO> listAttachments(String ownerType, Long ownerId) {
        List<DeclarationAttachment> list = attachmentMapper.selectList(new LambdaQueryWrapper<DeclarationAttachment>()
                .eq(DeclarationAttachment::getOwnerType, ownerType)
                .eq(DeclarationAttachment::getOwnerId, ownerId)
                .orderByAsc(DeclarationAttachment::getId));
        List<DeclarationDetailVO.AttachmentVO> out = new ArrayList<>();
        for (DeclarationAttachment a : list) {
            DeclarationDetailVO.AttachmentVO vo = new DeclarationDetailVO.AttachmentVO();
            vo.setId(a.getId());
            vo.setAttachmentType(a.getAttachmentType());
            vo.setFileName(a.getFileName());
            vo.setFileKey(a.getFileKey());
            vo.setBucket(a.getBucket());
            vo.setFileSize(a.getFileSize());
            vo.setRemark(a.getRemark());
            vo.setCreateTime(a.getCreateTime() == null ? null : a.getCreateTime().toString());
            out.add(vo);
        }
        return out;
    }

    /**
     * 批量填农户明细审核人姓名（一次 selectBatchIds 替代 N 次 selectById）
     */
    private void fillHouseholdAuditByName(List<DeclarationDetailVO.HouseholdVO> list) {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream().map(DeclarationDetailVO.HouseholdVO::getAuditBy).filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return;
        java.util.Map<Long, String> map = userMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u.getRealName() == null ? u.getUsername() : u.getRealName(), (a, b) -> a));
        for (DeclarationDetailVO.HouseholdVO v : list) {
            if (v.getAuditBy() != null) {
                v.setAuditByName(map.getOrDefault(v.getAuditBy(), "未知"));
            }
        }
    }
    /** 批量填公建设施明细审核人姓名 */
    private void fillFacilityAuditByName(List<DeclarationDetailVO.FacilityVO> list) {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream().map(DeclarationDetailVO.FacilityVO::getAuditBy).filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return;
        java.util.Map<Long, String> map = userMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u.getRealName() == null ? u.getUsername() : u.getRealName(), (a, b) -> a));
        for (DeclarationDetailVO.FacilityVO v : list) {
            if (v.getAuditBy() != null) {
                v.setAuditByName(map.getOrDefault(v.getAuditBy(), "未知"));
            }
        }
    }

    /**
     * 列表场景：Declaration → DeclarationDetailVO（不查明细，减少 N+1）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editHouseholdDetail(DeclarationDetailEditDTO dto) {
        // 1. 明细存在性 + 归属申报单校验（防止串数据）
        DeclarationHousehold hh = householdMapper.selectById(dto.getDetailId());
        if (hh == null) {
            throw new BizException("农户明细不存在或已删除");
        }
        if (!hh.getDeclarationId().equals(dto.getDeclarationId())) {
            throw new BizException("该农户明细不属于此申报单");
        }
        Declaration parent = declarationMapper.selectById(hh.getDeclarationId());
        if (parent == null) {
            throw new BizException("所属申报单不存在");
        }
        // 仅草稿 / 待审核状态允许修改明细
        if ("approved".equals(parent.getStatus()) || "rejected".equals(parent.getStatus())) {
            throw new BizException("已通过/已驳回的申报单不允许修改明细");
        }

        // 2. 先存 DB 原值（对比 diff 用），再更新单户金额 + 事项说明
        String oldAmount = hh.getAmount() == null ? null : hh.getAmount().toPlainString();
        String oldRemark = hh.getRemark();
        String beforeAuditStatus = hh.getAuditStatus();
        hh.setAmount(dto.getAmount());
        hh.setRemark(trimToNull(dto.getRemark()));
        householdMapper.updateById(hh);

        // 3. 明细级附件：owner_type=household, owner_id=明细ID；返回 true 表示有增删改
        boolean attChanged = syncDetailAttachments("household", hh.getId(), dto.getAttachments());

        // 4. 写 USER_EDIT 日志的守卫条件：
        //    ❌ 从未提交过审核流程的草稿阶段（parent=draft + 明细 audit_status=unsubmitted）→ 不写
        //       因为用户新建单据后反复填改金额、换附件，这些"草稿修改"全部不进时间线。
        //    ✅ 以下场景才写：
        //       (a) 明细被驳回 / 撤销过（beforeAuditStatus=rejected / 曾被 approve 后 revoke）
        //       (b) 主表已提交过（parent.status=submitted / approved / established / rejected 等）
        //       判定方式：neverSubmitted = parent.draft && before=unsubmitted  → 取反
        boolean neverSubmitted = "draft".equals(parent.getStatus()) && "unsubmitted".equals(beforeAuditStatus);
        String newAmount = hh.getAmount() == null ? null : hh.getAmount().toPlainString();
        String fieldDiffJson = buildFieldDiff(oldAmount, newAmount, oldRemark, hh.getRemark());
        if (!neverSubmitted && (fieldDiffJson != null || attChanged)) {
            LoginUser me = UserContext.get();
            String name = me == null ? null : (me.getRealName() == null ? me.getUsername() : me.getRealName());
            writeUserEditLog(
                    hh.getDeclarationId(), "household", hh.getId(),
                    hh.getSnapshotHeadName(),
                    beforeAuditStatus, hh.getAuditStatus(),
                    fieldDiffJson, attChanged,
                    null,
                    me == null ? null : me.getUserId(),
                    name);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPublicFacilityDetail(DeclarationDetailEditDTO dto) {
        DeclarationPublicFacility pf = facilityMapper.selectById(dto.getDetailId());
        if (pf == null) {
            throw new BizException("公建设施明细不存在或已删除");
        }
        if (!pf.getDeclarationId().equals(dto.getDeclarationId())) {
            throw new BizException("该公建设施明细不属于此申报单");
        }
        Declaration parent = declarationMapper.selectById(pf.getDeclarationId());
        if (parent == null) {
            throw new BizException("所属申报单不存在");
        }
        if ("approved".equals(parent.getStatus()) || "rejected".equals(parent.getStatus())) {
            throw new BizException("已通过/已驳回的申报单不允许修改明细");
        }

        // 2. 先存 DB 原值（对比 diff 用），再更新
        String oldAmount = pf.getAmount() == null ? null : pf.getAmount().toPlainString();
        String oldRemark = pf.getRemark();
        String beforeAuditStatus = pf.getAuditStatus();
        pf.setAmount(dto.getAmount());
        pf.setRemark(trimToNull(dto.getRemark()));
        facilityMapper.updateById(pf);
        boolean attChanged = syncDetailAttachments("public_facility", pf.getId(), dto.getAttachments());

        // 3. USER_EDIT 日志守卫：草稿+未提交 → 不写
        boolean neverSubmitted = "draft".equals(parent.getStatus()) && "unsubmitted".equals(beforeAuditStatus);
        String newAmount = pf.getAmount() == null ? null : pf.getAmount().toPlainString();
        String fieldDiffJson = buildFieldDiff(oldAmount, newAmount, oldRemark, pf.getRemark());
        if (!neverSubmitted && (fieldDiffJson != null || attChanged)) {
            LoginUser me = UserContext.get();
            String name = me == null ? null : (me.getRealName() == null ? me.getUsername() : me.getRealName());
            writeUserEditLog(
                    pf.getDeclarationId(), "public_facility", pf.getId(),
                    pf.getSnapshotFacilityName(),
                    beforeAuditStatus, pf.getAuditStatus(),
                    fieldDiffJson, attChanged,
                    null,
                    me == null ? null : me.getUserId(),
                    name);
        }
    }

    /**
     * 明细级附件同步：对比已有附件做增删改
     * 和主表级附件不同的是 owner_type 用 "household" / "public_facility"（不是 "declaration"）
     */
    /**
     * 同步明细级附件：逻辑删除前端没回传的 + 前端回传的做 insert/update。
     * @return true 表示附件有增删改（前端没传 / 有删除 / 有新增 / 有字段更新），false 表示附件没变
     */
    private boolean syncDetailAttachments(String ownerType, Long ownerId,
                                          List<DeclarationDetailEditDTO.AttachmentRow> attachments) {
        // 先查已有附件
        List<DeclarationAttachment> existList = attachmentMapper.selectList(
                new LambdaQueryWrapper<DeclarationAttachment>()
                        .eq(DeclarationAttachment::getOwnerType, ownerType)
                        .eq(DeclarationAttachment::getOwnerId, ownerId));
        Set<Long> inputIds = new java.util.HashSet<>();
        int inputSize = 0;
        if (attachments != null) {
            inputSize = attachments.size();
            for (DeclarationDetailEditDTO.AttachmentRow row : attachments) {
                if (row != null && row.getAttachmentId() != null) {
                    inputIds.add(row.getAttachmentId());
                }
            }
        }
        // 逻辑删除前端没回传的附件（前端把已存在的都带上了，没带上就是要删的）
        boolean changed = false;
        for (DeclarationAttachment att : existList) {
            if (!inputIds.contains(att.getId())) {
                attachmentMapper.deleteById(att.getId());
                changed = true;
            }
        }
        // 数量变化 → 一定有增有删
        if (attachments == null) return changed || !existList.isEmpty();
        if (inputSize != existList.size()) changed = true;
        // 遍历前端回传：已有附件更新，新增附件插
        for (DeclarationDetailEditDTO.AttachmentRow row : attachments) {
            if (row == null) continue;
            if (row.getAttachmentId() != null) {
                DeclarationAttachment att = attachmentMapper.selectById(row.getAttachmentId());
                if (att != null) {
                    if (!Objects.equals(trimToNull(row.getAttachmentType()), trimToNull(att.getAttachmentType()))
                            || !Objects.equals(trimToNull(row.getRemark()), trimToNull(att.getRemark()))) {
                        att.setAttachmentType(trimToNull(row.getAttachmentType()));
                        att.setRemark(trimToNull(row.getRemark()));
                        attachmentMapper.updateById(att);
                        changed = true;
                    }
                    continue;
                } else {
                    // attachmentId 带了但 DB 查不到 → 当成新增
                    changed = true;
                }
            } else {
                changed = true; // 没带 attachmentId → 新增附件
            }
            DeclarationAttachment att = new DeclarationAttachment();
            att.setOwnerType(ownerType);
            att.setOwnerId(ownerId);
            att.setAttachmentType(trimToNull(row.getAttachmentType()));
            att.setFileName(trimToNull(row.getFileName()));
            att.setFileKey(trimToNull(row.getFileKey()));
            att.setBucket(trimToNull(row.getBucket()));
            att.setFileSize(row.getFileSize());
            att.setRemark(trimToNull(row.getRemark()));
            att.setCreateBy(currentUser());
            att.setCreateTime(LocalDateTime.now());
            attachmentMapper.insert(att);
        }
        return changed;
    }

    private DeclarationDetailVO toSimpleVO(Declaration d) {
        DeclarationDetailVO vo = new DeclarationDetailVO();
        vo.setId(d.getId());
        vo.setBusinessCode(d.getBusinessCode());
        vo.setDeclarationDate(d.getDeclarationDate());
        vo.setTitle(d.getTitle());
        vo.setTownCode(d.getTownCode());
        vo.setTownName(d.getTownName());
        vo.setVillageGroupCode(d.getVillageGroupCode());
        vo.setVillageGroupName(d.getVillageGroupName());
        vo.setDeclarationType(d.getDeclarationType());
        vo.setDeclarationCategory(d.getDeclarationCategory());
        vo.setAmount(d.getAmount());
        vo.setRemark(d.getRemark());
        vo.setStatus(d.getStatus());
        vo.setRecipientUserId(d.getRecipientUserId());
        vo.setAuditBy(d.getAuditBy());
        vo.setAuditTime(d.getAuditTime());
        vo.setAuditRemark(d.getAuditRemark());
        vo.setCreateBy(d.getCreateBy());
        vo.setCreateTime(d.getCreateTime());
        vo.setUpdateBy(d.getUpdateBy());
        vo.setUpdateTime(d.getUpdateTime());
        return vo;
    }

    /**
     * 递归收集指定区划 code + 所有下级区划 code
     * 用途：
     *   前端选村（level=5） → 要查这个村本身 + 下属所有组（level=6）的农户/公建设施
     *   前端选组（level=6） → 只查这个组本身
     * 因为户基础信息表和公建设施表都是按 region_code 精确存的，用 IN(...) 一次性捞出来
     */
    private Set<String> collectAllDescendantCodes(String rootCode) {
        Set<String> sink = new HashSet<>();
        sink.add(rootCode);
        collectChildrenCodes(rootCode, sink);
        return sink;
    }

    private void collectChildrenCodes(String parentCode, Set<String> sink) {
        List<SysRegion> children = regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getParentCode, parentCode));
        for (SysRegion c : children) {
            sink.add(c.getCode());
            collectChildrenCodes(c.getCode(), sink);
        }
    }

    /**
     * 字典标签查询占位方法
     * 目前前端统一做字典翻译（getDictByType），后端先原样返回 dictValue，后续如需服务端翻译可注入 SysDictMapper
     */
    @SuppressWarnings("unused")
    private String dictLabelOf(String dictType, String dictValue) {
        return dictValue;
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

    // ======================== 明细级审核 / 用户修改历史日志 ========================
    // 统一入口：auditDetail / revertDetail / editHouseholdDetail / editPublicFacilityDetail / submit 里 reset rejected→pending
    // 所有 insert 都用这同一个方法，保证字段不缺、快照名不丢。

    /**
     * 写一条"审核类"日志（AUDIT_PASS / AUDIT_REJECT / AUDIT_REVOKE / SUBMIT_UNSUBMITTED）。
     * @param action           动作类型常量（见 Entity 注释）
     * @param declarationId    申报单 ID
     * @param detailKind       household / public_facility
     * @param detailId         明细 ID
     * @param detailSnapshotName 快照展示名（冗余，从明细实体拿）
     * @param beforeStatus     操作前 audit_status（String 或 null）
     * @param afterStatus      操作后 audit_status（String 或 null）
     * @param remark           审核意见 / reset 说明（可为 null）
     * @param operatorId       操作人 ID（审核人或申报人）
     * @param operatorName     操作人姓名（调用方自己好传）
     */
    private void writeAuditLog(String action, Long declarationId, String detailKind, Long detailId,
                                String detailSnapshotName, String beforeStatus, String afterStatus,
                                String remark, Long operatorId, String operatorName) {
        DeclarationDetailAuditLog log = new DeclarationDetailAuditLog();
        log.setDeclarationId(declarationId);
        log.setDetailKind(detailKind);
        log.setDetailId(detailId);
        log.setDetailSnapshotName(detailSnapshotName);
        log.setAction(action);
        log.setBeforeAuditStatus(beforeStatus);
        log.setAfterAuditStatus(afterStatus);
        log.setRemark(trimToNull(remark));
        log.setFieldDiff(null);                 // 审核类动作为 null
        log.setAttachmentChanged(false);        // 审核不动附件
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    /**
     * 写一条"用户修改明细"日志（USER_EDIT）。
     * fieldDiffJson 是调用方自己拼的 JSON 字符串，比如：
     *   [{"field":"amount","old":"200.00","new":"300.00"},
     *    {"field":"remark","old":null,"new":"补充材料齐全"}]
     */
    private void writeUserEditLog(Long declarationId, String detailKind, Long detailId,
                                  String detailSnapshotName, String beforeStatus, String afterStatus,
                                  String fieldDiffJson,
                                  boolean attachmentChanged, String remark,
                                  Long operatorId, String operatorName) {
        DeclarationDetailAuditLog log = new DeclarationDetailAuditLog();
        log.setDeclarationId(declarationId);
        log.setDetailKind(detailKind);
        log.setDetailId(detailId);
        log.setDetailSnapshotName(detailSnapshotName);
        log.setAction("USER_EDIT");
        log.setBeforeAuditStatus(beforeStatus);
        log.setAfterAuditStatus(afterStatus);
        log.setRemark(trimToNull(remark));
        log.setFieldDiff(fieldDiffJson);
        log.setAttachmentChanged(attachmentChanged);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    /**
     * 构造 USER_EDIT 的字段 diff JSON 字符串。
     * 对比 amount / remark 两个字段，哪些变了就 push 哪个。
     * 返回 null 表示没东西变更（调用方据此决定要不要写 USER_EDIT 日志）。
     *
     * 完全手工拼 JSON，避免引入 Jackson / Fastjson / Hutool。
     * JSON 规范：字符串双引号 + 特殊字符转义 + null 原样输出。
     */
    private String buildFieldDiff(String oldAmount, String newAmount, String oldRemark, String newRemark) {
        boolean amountChanged = !Objects.equals(oldAmount, newAmount);
        boolean remarkChanged = !Objects.equals(trimToNull(oldRemark), trimToNull(newRemark));
        if (!amountChanged && !remarkChanged) return null;

        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        if (amountChanged) {
            sb.append(buildFieldNode("amount", oldAmount, newAmount));
            first = false;
        }
        if (remarkChanged) {
            if (!first) sb.append(',');
            sb.append(buildFieldNode("remark", oldRemark, newRemark));
        }
        sb.append(']');
        return sb.toString();
    }

    /** 把一个字段变成 JSON 对象：{"field":"xxx","old":"yyy","new":"zzz"} 或 null */
    private StringBuilder buildFieldNode(String field, String oldVal, String newVal) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"field\":\"").append(field).append("\",\"old\":").append(toJsonValue(oldVal));
        sb.append(",\"new\":").append(toJsonValue(newVal)).append('}');
        return sb;
    }

    /** 安全地把一个 Java String 变 JSON 值：null → null，字符串 → "xxx"（含转义） */
    private String toJsonValue(String v) {
        if (v == null) return "null";
        return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * 获取指定申报单的全部明细审核/修改历史（时间线）
     * 批量查 operator 姓名、按 create_time 升序返回。
     */
    @Override
    public List<DeclarationDetailAuditLogVO> auditLogs(Long declarationId) {
        Declaration parent = declarationMapper.selectById(declarationId);
        if (parent == null) throw new BizException("申报单不存在");

        List<DeclarationDetailAuditLog> list = auditLogMapper.selectList(
                new LambdaQueryWrapper<DeclarationDetailAuditLog>()
                        .eq(DeclarationDetailAuditLog::getDeclarationId, declarationId)
                        .orderByAsc(DeclarationDetailAuditLog::getCreateTime));
        if (list.isEmpty()) return java.util.Collections.emptyList();

        // 批量查操作人姓名（避免 N+1）
        List<Long> operatorIds = list.stream().map(DeclarationDetailAuditLog::getOperatorId)
                .filter(Objects::nonNull).distinct().toList();
        java.util.Map<Long, String> nameMap = operatorIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : batchUserNames(operatorIds);

        // 组装 VO
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return list.stream().map(l -> {
            DeclarationDetailAuditLogVO v = new DeclarationDetailAuditLogVO();
            v.setId(l.getId());
            v.setDeclarationId(l.getDeclarationId());
            v.setDetailKind(l.getDetailKind());
            v.setDetailId(l.getDetailId());
            v.setDetailSnapshotName(l.getDetailSnapshotName());
            v.setAction(l.getAction());
            v.setBeforeAuditStatus(l.getBeforeAuditStatus());
            v.setAfterAuditStatus(l.getAfterAuditStatus());
            v.setRemark(l.getRemark());
            v.setFieldDiff(l.getFieldDiff());
            v.setAttachmentChanged(l.getAttachmentChanged() != null && l.getAttachmentChanged());
            v.setOperatorId(l.getOperatorId());
            v.setOperatorName(l.getOperatorName() != null ? l.getOperatorName() : nameMap.get(l.getOperatorId()));
            v.setCreateTime(l.getCreateTime() == null ? null : l.getCreateTime().format(f));
            return v;
        }).toList();
    }

    /**
     * 批量查 userId → 姓名 Map（selectBatchIds 替代 N 次 selectById）
     */
    private java.util.Map<Long, String> batchUserNames(java.util.List<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyMap();
        return userMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        SysUser::getId,
                        u -> u.getRealName() == null ? u.getUsername() : u.getRealName(),
                        (a, b) -> a));
    }

    /** auditDetail 的 action 参数 → 日志表 action 常量 */
    private static String auditLogAction(String auditDetailAction) {
        return switch (auditDetailAction) {
            case "approve" -> "AUDIT_PASS";
            case "reject"  -> "AUDIT_REJECT";
            case "revoke"  -> "AUDIT_REVOKE";
            default        -> "UNKNOWN_" + auditDetailAction;
        };
    }

    /** 根据 action 映射成前端看得懂的中文标签 */
    public static String actionLabel(String action) {
        if (action == null) return "";
        return switch (action) {
            case "AUDIT_PASS"          -> "审核通过";
            case "AUDIT_REJECT"        -> "审核驳回";
            case "AUDIT_REVOKE"        -> "审核撤销";
            case "SUBMIT_FIRST"        -> "首次提交审核";
            case "SUBMIT_RESUBMITTED"  -> "重新提交审核";
            case "USER_EDIT"           -> "用户修改明细";
            default -> action;
        };
    }

    /** 审批状态值 → 中文标签（按明细表 audit_status 维度） */
    private static String mapAuditStatusLabel(String auditStatus) {
        if ("pending".equals(auditStatus))  return "待审核";
        if ("approved".equals(auditStatus)) return "已通过";
        if ("rejected".equals(auditStatus)) return "已驳回";
        if ("established".equals(auditStatus)) return "已立项";
        if ("unsubmitted".equals(auditStatus)) return "未提交";
        return auditStatus == null ? "" : auditStatus;
    }

    /** 排序 key 生成器（统一三个维度） */
    private static List<String> orderedKeys(Set<String> keys, String groupBy) {
        List<String> result = new ArrayList<>();
        if ("category".equals(groupBy)) {
            for (String k : new String[]{"1", "2"}) if (keys.contains(k)) result.add(k);
            for (String k : keys) if (!"1".equals(k) && !"2".equals(k)) result.add(k);
        } else if ("status".equals(groupBy)) {
            for (String k : new String[]{"pending", "approved", "rejected", "established"})
                if (keys.contains(k)) result.add(k);
            for (String k : keys) {
                boolean already = false;
                for (String o : new String[]{"pending", "approved", "rejected", "established"})
                    if (o.equals(k)) { already = true; break; }
                if (!already) result.add(k);
            }
        } else { // village
            result.addAll(keys);
            java.util.Collections.sort(result);
        }
        return result;
    }

    /** 审批状态 / 分组标签映射 */
    private static String auditStatusToLabel(String s) {
        return mapAuditStatusLabel(s == null ? "" : s);
    }

    // ================ 申报综合查询统计 ================

    /** 明细级扁平条目（把 household + facility 统一抽象成同结构，方便任意维度 groupBy） */
    private static class FlatItem {
        Long declarationId;           // 所属申报单（用于"申报单数"去重）
        String declarationCategory;   // 1=到户 / 2=公建
        String auditStatus;           // 明细审批状态（pending/approved/rejected/established）
        String villageGroupCode;      // 申报单所属村组
        String villageGroupName;
        Long householdId;             // 非 null 表示农户明细
        Long facilityId;              // 非 null 表示公建设施明细
        java.math.BigDecimal amount;  // 明细金额
    }

    @Override
    public DeclarationStatisticsVO statistics(String year, String regionCode, String groupBy,
                                              String declarationCategory, String status, String villageGroupCode) {
        DeclarationStatisticsVO vo = new DeclarationStatisticsVO();
        vo.setGroupBy(groupBy == null || groupBy.isBlank() ? "category" : groupBy);

        // 1) 前置过滤：按顶部条件过滤 declaration 主表
        LambdaQueryWrapper<Declaration> wrapper = new LambdaQueryWrapper<>();
        if (year != null && !year.isBlank()) {
            try {
                int y = Integer.parseInt(year.trim());
                wrapper.apply("YEAR(create_time) = {0}", y);
            } catch (NumberFormatException ignored) { }
        }
        if (regionCode != null && !regionCode.isBlank()) {
            Set<String> regionCodes = collectDescendantRegionCodes(regionCode.trim());
            if (!regionCodes.isEmpty()) wrapper.in(Declaration::getVillageGroupCode, regionCodes);
            else wrapper.eq(Declaration::getVillageGroupCode, regionCode.trim());
        }
        // 注意：左侧三个筛选条件都是"在已选中分组维度内的子筛选"，
        // 跟 groupBy 联动——groupBy==category 时 declarationCategory 生效、
        // groupBy==status 时 status（明细 audit_status）生效、
        // groupBy==village 时 villageGroupCode 生效
        if ("category".equals(groupBy) && declarationCategory != null && !declarationCategory.isBlank()) {
            wrapper.eq(Declaration::getDeclarationCategory, declarationCategory.trim());
        }
        if ("village".equals(groupBy) && villageGroupCode != null && !villageGroupCode.isBlank()) {
            wrapper.eq(Declaration::getVillageGroupCode, villageGroupCode.trim());
        }
        wrapper.orderByDesc(Declaration::getCreateTime);
        List<Declaration> declarations = declarationMapper.selectList(wrapper);

        // 2) 构建扁平明细列表（ household + facility 拍平成 FlatItem）
        Set<Long> declIds = declarations.stream().map(Declaration::getId).collect(Collectors.toSet());
        // 主表查找 declId → 主表对象（用于回填 category/village 信息）
        Map<Long, Declaration> declMap = declarations.stream()
                .collect(Collectors.toMap(Declaration::getId, d -> d, (a, b) -> a));

        // 查全部 household（按声明过滤后再按 status 子筛选）
        List<DeclarationHousehold> hhList = declIds.isEmpty()
                ? new ArrayList<>()
                : householdMapper.selectList(new LambdaQueryWrapper<DeclarationHousehold>()
                        .in(DeclarationHousehold::getDeclarationId, declIds)
                        .eq(DeclarationHousehold::getDeleted, 0));
        List<DeclarationPublicFacility> pfList = declIds.isEmpty()
                ? new ArrayList<>()
                : facilityMapper.selectList(new LambdaQueryWrapper<DeclarationPublicFacility>()
                        .in(DeclarationPublicFacility::getDeclarationId, declIds)
                        .eq(DeclarationPublicFacility::getDeleted, 0));

        // 按 status 子筛选过滤明细（只有 groupBy==status 时才生效）
        if ("status".equals(groupBy) && status != null && !status.isBlank()) {
            hhList = hhList.stream().filter(h -> status.equals(h.getAuditStatus())).collect(Collectors.toList());
            pfList = pfList.stream().filter(f -> status.equals(f.getAuditStatus())).collect(Collectors.toList());
        }

        // 拍平成 FlatItem
        List<FlatItem> flatItems = new ArrayList<>();
        for (DeclarationHousehold hh : hhList) {
            Declaration d = declMap.get(hh.getDeclarationId());
            if (d == null) continue;
            FlatItem f = new FlatItem();
            f.declarationId = d.getId();
            f.declarationCategory = d.getDeclarationCategory();
            f.auditStatus = hh.getAuditStatus();
            f.villageGroupCode = d.getVillageGroupCode();
            f.villageGroupName = d.getVillageGroupName();
            f.householdId = hh.getHouseholdId();
            f.facilityId = null;
            f.amount = hh.getAmount() != null ? hh.getAmount() : java.math.BigDecimal.ZERO;
            flatItems.add(f);
        }
        for (DeclarationPublicFacility pf : pfList) {
            Declaration d = declMap.get(pf.getDeclarationId());
            if (d == null) continue;
            FlatItem f = new FlatItem();
            f.declarationId = d.getId();
            f.declarationCategory = d.getDeclarationCategory();
            f.auditStatus = pf.getAuditStatus();
            f.villageGroupCode = d.getVillageGroupCode();
            f.villageGroupName = d.getVillageGroupName();
            f.householdId = null;
            f.facilityId = pf.getFacilityId();
            f.amount = pf.getAmount() != null ? pf.getAmount() : java.math.BigDecimal.ZERO;
            flatItems.add(f);
        }

        // 3) 按 groupBy 分组 → 再按村组拆二级 → 构建 rows
        List<DeclarationStatisticsVO.Row> rows = new ArrayList<>();

        // 主分组：从 FlatItem 提取 key
        Map<String, List<FlatItem>> mainGroup = new HashMap<>();
        Map<String, String> mainLabel = new HashMap<>();
        for (FlatItem f : flatItems) {
            String key;
            String label;
            if ("status".equals(groupBy)) {
                key = f.auditStatus == null ? "" : f.auditStatus;
                label = auditStatusToLabel(key);
            } else if ("village".equals(groupBy)) {
                key = f.villageGroupCode == null ? "" : f.villageGroupCode;
                label = f.villageGroupName == null ? (key.isEmpty() ? "未知村组" : key) : f.villageGroupName;
            } else { // category
                key = f.declarationCategory == null ? "" : f.declarationCategory;
                label = mapCategoryLabel(key);
            }
            mainLabel.putIfAbsent(key, label);
            mainGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(f);
        }
        List<String> orderedMainKeys = orderedKeys(mainGroup.keySet(), groupBy);

        // 全局总计（申报单数按 distinct declarationId；户数/设施数不去重累加）
        Set<Long> grandDeclIds = new HashSet<>();
        int grandHouseholdCount = 0;
        int grandFacilityCount = 0;
        java.math.BigDecimal grandAmount = java.math.BigDecimal.ZERO;
        for (FlatItem f : flatItems) {
            if (f.declarationId != null) grandDeclIds.add(f.declarationId);
            if (f.householdId != null) grandHouseholdCount++;
            if (f.facilityId != null) grandFacilityCount++;
            if (f.amount != null) grandAmount = grandAmount.add(f.amount);
        }

        for (String mainKey : orderedMainKeys) {
            List<FlatItem> groupItems = mainGroup.get(mainKey);
            String mainLbl = mainLabel.getOrDefault(mainKey, mainKey);

            // 二级：按 villageGroupCode 再拆
            Map<String, List<FlatItem>> byVillage = new HashMap<>();
            Map<String, String> villageNameMap = new HashMap<>();
            for (FlatItem f : groupItems) {
                String vgKey = f.villageGroupCode == null ? "" : f.villageGroupCode;
                byVillage.computeIfAbsent(vgKey, k -> new ArrayList<>()).add(f);
                villageNameMap.putIfAbsent(vgKey, f.villageGroupName == null ? (vgKey.isEmpty() ? "未知村组" : vgKey) : f.villageGroupName);
            }
            List<String> vgKeys = new ArrayList<>(byVillage.keySet());
            java.util.Collections.sort(vgKeys);

            // 分组汇总行（一级）
            DeclarationStatisticsVO.Row summaryRow = buildRow(groupItems, mainLbl, "全部村组", null, true, false);
            rows.add(summaryRow);

            // 二级：村组明细行
            for (String vgKey : vgKeys) {
                List<FlatItem> vgItems = byVillage.get(vgKey);
                DeclarationStatisticsVO.Row detailRow = buildRow(vgItems, "",
                        villageNameMap.getOrDefault(vgKey, vgKey),
                        vgKey.isEmpty() ? null : vgKey, false, false);
                rows.add(detailRow);
            }
        }

        // 总计行
        DeclarationStatisticsVO.Row totalRow = new DeclarationStatisticsVO.Row();
        totalRow.setGroupSummary(true);
        totalRow.setGrandTotal(true);
        totalRow.setGroupLabel("总计");
        totalRow.setVillageName("全部区划");
        totalRow.setDeclarationCount(grandDeclIds.size());
        totalRow.setHouseholdCount(grandHouseholdCount);
        totalRow.setFacilityCount(grandFacilityCount);
        totalRow.setAmount(grandAmount);
        rows.add(totalRow);

        vo.setRows(rows);

        // 4) sidebar（左侧筛选面板）统计 —— 申报单数按 distinct declarationId
        DeclarationStatisticsVO.SidebarStats sidebar = new DeclarationStatisticsVO.SidebarStats();

        // 按申报类别
        List<DeclarationStatisticsVO.StatItem> byCategory = new ArrayList<>();
        Map<String, Set<Long>> catDecl = new HashMap<>();
        for (FlatItem f : flatItems) {
            String c = f.declarationCategory == null ? "" : f.declarationCategory;
            catDecl.computeIfAbsent(c, k -> new HashSet<>());
            if (f.declarationId != null) catDecl.get(c).add(f.declarationId);
        }
        for (String c : new String[]{"1", "2"}) {
            DeclarationStatisticsVO.StatItem it = new DeclarationStatisticsVO.StatItem();
            it.setValue(c);
            it.setLabel(mapCategoryLabel(c));
            it.setCount(catDecl.getOrDefault(c, Collections.emptySet()).size());
            byCategory.add(it);
        }
        sidebar.setByCategory(byCategory);

        // 按审批状态（固定四项）
        List<DeclarationStatisticsVO.StatItem> byStatus = new ArrayList<>();
        Map<String, Set<Long>> statusDecl = new HashMap<>();
        for (FlatItem f : flatItems) {
            String s = f.auditStatus == null ? "" : f.auditStatus;
            statusDecl.computeIfAbsent(s, k -> new HashSet<>());
            if (f.declarationId != null) statusDecl.get(s).add(f.declarationId);
        }
        for (String s : new String[]{"pending", "approved", "rejected", "established"}) {
            DeclarationStatisticsVO.StatItem it = new DeclarationStatisticsVO.StatItem();
            it.setValue(s);
            it.setLabel(auditStatusToLabel(s));
            it.setCount(statusDecl.getOrDefault(s, Collections.emptySet()).size());
            byStatus.add(it);
        }
        sidebar.setByStatus(byStatus);

        // 按所属村组
        List<DeclarationStatisticsVO.StatItem> byVillage = new ArrayList<>();
        Map<String, Set<Long>> vgDecl = new HashMap<>();
        Map<String, String> vgNameMap = new HashMap<>();
        for (FlatItem f : flatItems) {
            String vg = f.villageGroupCode == null ? "" : f.villageGroupCode;
            vgDecl.computeIfAbsent(vg, k -> new HashSet<>());
            if (f.declarationId != null) vgDecl.get(vg).add(f.declarationId);
            vgNameMap.putIfAbsent(vg, f.villageGroupName == null ? (vg.isEmpty() ? "未知村组" : vg) : f.villageGroupName);
        }
        List<String> vgKeysSorted = new ArrayList<>(vgDecl.keySet());
        java.util.Collections.sort(vgKeysSorted, (a, b) -> vgNameMap.getOrDefault(a, a).compareTo(vgNameMap.getOrDefault(b, b)));
        for (String vg : vgKeysSorted) {
            DeclarationStatisticsVO.StatItem it = new DeclarationStatisticsVO.StatItem();
            it.setValue(vg.isEmpty() ? null : vg);
            it.setLabel(vgNameMap.getOrDefault(vg, vg));
            it.setCount(vgDecl.getOrDefault(vg, Collections.emptySet()).size());
            byVillage.add(it);
        }
        sidebar.setByVillage(byVillage);

        vo.setSidebar(sidebar);
        return vo;
    }

    /** 把一组 FlatItem 聚合成一条 Row（用于分组汇总行 / 村组明细行） */
    private DeclarationStatisticsVO.Row buildRow(List<FlatItem> items, String groupLabel,
                                                  String villageName, String villageGroupCode,
                                                  boolean groupSummary, boolean grandTotal) {
        DeclarationStatisticsVO.Row row = new DeclarationStatisticsVO.Row();
        row.setGroupSummary(groupSummary);
        row.setGrandTotal(grandTotal);
        row.setGroupLabel(groupLabel);
        row.setVillageName(villageName);
        row.setVillageGroupCode(villageGroupCode);
        Set<Long> declIds = new HashSet<>();
        int hhCount = 0;
        int pfCount = 0;
        java.math.BigDecimal amt = java.math.BigDecimal.ZERO;
        for (FlatItem f : items) {
            if (f.declarationId != null) declIds.add(f.declarationId);
            if (f.householdId != null) hhCount++;
            if (f.facilityId != null) pfCount++;
            if (f.amount != null) amt = amt.add(f.amount);
        }
        row.setDeclarationCount(declIds.size());
        row.setHouseholdCount(hhCount);
        row.setFacilityCount(pfCount);
        row.setAmount(amt);
        return row;
    }

    /** 取给定 regionCode 及其全部子孙级区划 code（用于过滤 Declaration.village_group_code） */
    private Set<String> collectDescendantRegionCodes(String regionCode) {
        Set<String> result = new HashSet<>();
        if (regionCode == null || regionCode.isBlank()) return result;
        // 先把自身加进去
        result.add(regionCode);
        // 递归查子级（parent_code = 任意一个已收集 code）
        Set<String> current = new HashSet<>();
        current.add(regionCode);
        while (!current.isEmpty()) {
            List<SysRegion> children = regionMapper.selectList(
                    new LambdaQueryWrapper<SysRegion>().in(SysRegion::getParentCode, current));
            if (children.isEmpty()) break;
            Set<String> next = new HashSet<>();
            for (SysRegion r : children) {
                if (r.getCode() != null && result.add(r.getCode())) {
                    next.add(r.getCode());
                }
            }
            current = next;
        }
        return result;
    }

    /** 申报类别值 → 中文标签 */
    private static String mapCategoryLabel(String category) {
        if ("1".equals(category)) return "到户申报";
        if ("2".equals(category)) return "公建申报";
        return category == null ? "" : category;
    }

    /** 审批状态值 → 中文标签（主表 status 字段） */
    private static String mapStatusLabel(String status) {
        if ("submitted".equals(status)) return "待审核";
        if ("approved".equals(status)) return "已通过";
        if ("rejected".equals(status)) return "已驳回";
        if ("established".equals(status)) return "已立项";
        if ("draft".equals(status)) return "草稿";
        return status == null ? "" : status;
    }
}





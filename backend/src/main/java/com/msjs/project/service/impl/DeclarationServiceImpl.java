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
import com.msjs.project.entity.DeclarationHousehold;
import com.msjs.project.entity.DeclarationPublicFacility;
import com.msjs.project.entity.Household;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.entity.SysUser;
import com.msjs.project.mapper.BaseFacilityMapper;
import com.msjs.project.mapper.DeclarationAttachmentMapper;
import com.msjs.project.mapper.DeclarationHouseholdMapper;
import com.msjs.project.mapper.DeclarationMapper;
import com.msjs.project.mapper.DeclarationPublicFacilityMapper;
import com.msjs.project.mapper.HouseholdMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.mapper.SysUserMapper;
import com.msjs.project.service.DeclarationService;
import com.msjs.project.vo.DeclarationDetailVO;
import com.msjs.project.vo.UserRegionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
                hvo.setSnapshotHeadName(hh.getSnapshotHeadName());
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
        // 仅已通过/已驳回的申报单不允许编辑
        if ("approved".equals(exist.getStatus()) || "rejected".equals(exist.getStatus())) {
            throw new BizException("已通过/已驳回的申报单不允许编辑");
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
        householdMapper.update(null, new LambdaUpdateWrapper<DeclarationHousehold>()
                .eq(DeclarationHousehold::getDeclarationId, exist.getId())
                .in(DeclarationHousehold::getAuditStatus, "unsubmitted", "rejected")
                .set(DeclarationHousehold::getAuditStatus, "pending"));
        facilityMapper.update(null, new LambdaUpdateWrapper<DeclarationPublicFacility>()
                .eq(DeclarationPublicFacility::getDeclarationId, exist.getId())
                .in(DeclarationPublicFacility::getAuditStatus, "unsubmitted", "rejected")
                .set(DeclarationPublicFacility::getAuditStatus, "pending"));
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
        List<String> targetAuditStatuses = null;
        if ("submitted".equals(status)) targetAuditStatuses = Collections.singletonList("pending");
        else if ("approved".equals(status)) targetAuditStatuses = Collections.singletonList("approved");
        else if ("rejected".equals(status)) targetAuditStatuses = Collections.singletonList("rejected");

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
        } else {
            DeclarationPublicFacility pf = facilityMapper.selectById(dto.getDetailId());
            if (pf == null) {
                throw new BizException("公建设施明细不存在或已删除");
            }
            declarationId = pf.getDeclarationId();
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
        // 1. 把目标明细置回 pending + 清空审核痕迹
        if ("household".equals(dto.getDetailKind())) {
            DeclarationHousehold hh = householdMapper.selectById(dto.getDetailId());
            if (hh == null) throw new BizException("农户明细不存在或已删除");
            declarationId = hh.getDeclarationId();
            DeclarationHousehold update = new DeclarationHousehold();
            update.setId(hh.getId());
            update.setAuditStatus("pending");
            update.setAuditBy(null);
            update.setAuditTime(null);
            update.setAuditRemark(auditRemark);   // 退回原因留痕，给申报人看
            householdMapper.updateById(update);
        } else {
            DeclarationPublicFacility pf = facilityMapper.selectById(dto.getDetailId());
            if (pf == null) throw new BizException("公建设施明细不存在或已删除");
            declarationId = pf.getDeclarationId();
            DeclarationPublicFacility update = new DeclarationPublicFacility();
            update.setId(pf.getId());
            update.setAuditStatus("pending");
            update.setAuditBy(null);
            update.setAuditTime(null);
            update.setAuditRemark(auditRemark);
            facilityMapper.updateById(update);
        }

        // 2. 父申报单存在性校验
        Declaration parent = declarationMapper.selectById(declarationId);
        if (parent == null) throw new BizException("所属申报单不存在");
        // 注：不再强校验 recipient_user_id 必须等于当前登录人——"已通过/已驳回"页签里
        // 的单本身已经在 receivedPage 里按 recipient_user_id = me.getUserId() 过滤过了，
        // 二次校验是多余的；同时也方便管理员兜底退回。

        // 3. 父申报单回滚到 draft，清掉 recipient_user_id（让申报人能重新编辑、再次提交）
        Declaration parentUpdate = new Declaration();
        parentUpdate.setId(parent.getId());
        parentUpdate.setStatus("draft");
        parentUpdate.setRecipientUserId(null);
        parentUpdate.setAuditBy(null);
        parentUpdate.setAuditTime(null);
        parentUpdate.setAuditRemark(auditRemark != null ? ("明细退回：" + auditRemark) : "明细退回，退回到未提交");
        parentUpdate.setUpdateBy(currentUser());
        parentUpdate.setUpdateTime(LocalDateTime.now());
        declarationMapper.updateById(parentUpdate);
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
        // household 明细
        LambdaQueryWrapper<DeclarationHousehold> hw = new LambdaQueryWrapper<>();
        hw.select(DeclarationHousehold::getDeclarationId)
          .inSql(DeclarationHousehold::getAuditStatus, buildInExpr(targetAuditStatuses))
          .or(h -> h.isNull(DeclarationHousehold::getAuditStatus))
          .eq(DeclarationHousehold::getDeleted, 0);
        for (DeclarationHousehold h : householdMapper.selectList(hw)) {
            if (h.getDeclarationId() != null) result.add(h.getDeclarationId());
        }
        // public_facility 明细
        LambdaQueryWrapper<DeclarationPublicFacility> fw = new LambdaQueryWrapper<>();
        fw.select(DeclarationPublicFacility::getDeclarationId)
          .inSql(DeclarationPublicFacility::getAuditStatus, buildInExpr(targetAuditStatuses))
          .or(f -> f.isNull(DeclarationPublicFacility::getAuditStatus))
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
                hvo.setSnapshotHeadName(hh.getSnapshotHeadName());
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
            dhh.setSnapshotHeadName(h.getHeadName());
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

        // 2. 更新单户金额 + 事项说明
        hh.setAmount(dto.getAmount());
        hh.setRemark(trimToNull(dto.getRemark()));
        householdMapper.updateById(hh);

        // 3. 明细级附件：owner_type=household, owner_id=明细ID
        syncDetailAttachments("household", hh.getId(), dto.getAttachments());
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

        pf.setAmount(dto.getAmount());
        pf.setRemark(trimToNull(dto.getRemark()));
        facilityMapper.updateById(pf);
        syncDetailAttachments("public_facility", pf.getId(), dto.getAttachments());
    }

    /**
     * 明细级附件同步：对比已有附件做增删改
     * 和主表级附件不同的是 owner_type 用 "household" / "public_facility"（不是 "declaration"）
     */
    private void syncDetailAttachments(String ownerType, Long ownerId,
                                       List<DeclarationDetailEditDTO.AttachmentRow> attachments) {
        // 先查已有附件
        List<DeclarationAttachment> existList = attachmentMapper.selectList(
                new LambdaQueryWrapper<DeclarationAttachment>()
                        .eq(DeclarationAttachment::getOwnerType, ownerType)
                        .eq(DeclarationAttachment::getOwnerId, ownerId));
        Set<Long> inputIds = new java.util.HashSet<>();
        if (attachments != null) {
            for (DeclarationDetailEditDTO.AttachmentRow row : attachments) {
                if (row != null && row.getAttachmentId() != null) {
                    inputIds.add(row.getAttachmentId());
                }
            }
        }
        // 逻辑删除前端没回传的附件（前端把已存在的都带上了，没带上就是要删的）
        for (DeclarationAttachment att : existList) {
            if (!inputIds.contains(att.getId())) {
                attachmentMapper.deleteById(att.getId());
            }
        }
        if (attachments == null) {
            return;
        }
        // 遍历前端回传：已有附件更新，新增附件插
        for (DeclarationDetailEditDTO.AttachmentRow row : attachments) {
            if (row == null) continue;
            if (row.getAttachmentId() != null) {
                DeclarationAttachment att = attachmentMapper.selectById(row.getAttachmentId());
                if (att != null) {
                    att.setAttachmentType(trimToNull(row.getAttachmentType()));
                    att.setRemark(trimToNull(row.getRemark()));
                    attachmentMapper.updateById(att);
                    continue;
                }
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
    }

    private DeclarationDetailVO toSimpleVO(Declaration d) {
        DeclarationDetailVO vo = new DeclarationDetailVO();
        vo.setId(d.getId());
        vo.setBusinessCode(d.getBusinessCode());
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
}



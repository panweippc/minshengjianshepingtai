package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.common.BizException;
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
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.UserContext;
import com.msjs.project.service.ProjectApprovalService;
import com.msjs.project.vo.ProjectApprovalSubmitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 立项申请后端实现：
 *  - 事务：主表 + declare + detail + attachment 四张表原子写入
 *  - 事务：把选中 declaration 下的 approved 明细 audit_status → established
 *  - 事务：autoPromoteParentStatus（已在 DeclarationServiceImpl 里，这里直接复用其规则）
 * 附件不复用申报的附件表，用独立的 t_project_approval_attachment（bucket 默认 archive-formal）。
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
        approval.setStatus("pending");
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
            // 4.3 父申报单状态回算：established 也算"全部完成"，推进到 approved（或维持）
            d.setStatus("approved");
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
            int sort = 0;
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
                sort++;
            }
        }

        // 6. 校验：至少要有一条 approved 明细被立项，否则不给过
        if (detailCount == 0) {
            throw new BizException("所选申报单下没有已通过的明细可立项");
        }

        ProjectApprovalSubmitVO vo = new ProjectApprovalSubmitVO();
        vo.setId(approval.getId());
        vo.setApprovalCode(approval.getApprovalCode());
        vo.setStatus("pending");
        vo.setDeclareCount(declareCount);
        vo.setDetailCount(detailCount);
        vo.setAttachmentCount(attachmentCount);
        return vo;
    }

    // ============ helpers ============

    private ProjectApprovalDetail buildHouseholdDetail(Long declareId, DeclarationHousehold hh) {
        ProjectApprovalDetail pd = new ProjectApprovalDetail();
        pd.setDeclareId(declareId);
        pd.setDetailType("HOUSEHOLD");
        pd.setHouseholdId(hh.getId());
        pd.setTargetId(hh.getHouseholdId());
        pd.setTargetName(hh.getSnapshotHeadName());
        pd.setTargetCode(hh.getSnapshotHeadIdCard());
        pd.setRegionName(hh.getSnapshotRegionName());
        pd.setAmount(hh.getAmount() != null ? hh.getAmount() : BigDecimal.ZERO);
        pd.setCreateTime(LocalDateTime.now());
        return pd;
    }

    private ProjectApprovalDetail buildFacilityDetail(Long declareId, DeclarationPublicFacility pf) {
        ProjectApprovalDetail pd = new ProjectApprovalDetail();
        pd.setDeclareId(declareId);
        pd.setDetailType("PUBLIC_FACILITY");
        pd.setFacilityId(pf.getId());
        pd.setTargetId(pf.getFacilityId());
        pd.setTargetName(pf.getSnapshotFacilityName());
        pd.setTargetCode(pf.getSnapshotFacilityCode());
        pd.setRegionName(pf.getSnapshotRegionName());
        pd.setAmount(pf.getAmount() != null ? pf.getAmount() : BigDecimal.ZERO);
        pd.setCreateTime(LocalDateTime.now());
        return pd;
    }
}

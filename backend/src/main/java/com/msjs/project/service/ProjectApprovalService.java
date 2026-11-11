package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.ApprovalEditDTO;
import com.msjs.project.dto.ProjectApprovalSubmitDTO;
import com.msjs.project.vo.ApprovalAttachmentVO;
import com.msjs.project.vo.ApprovalCountVO;
import com.msjs.project.vo.ApprovalVO;
import com.msjs.project.vo.ProjectApprovalSubmitVO;

import java.util.List;

public interface ProjectApprovalService {
    /**
     * 提交立项申请：
     * 1. 保存立项主表 + declare + detail + attachment
     * 2. 把被选中的声明单下所有 approved 明细 audit_status 改成 established
     * 3. 重新计算父申报单状态（autoPromoteParentStatus 扩展支持 established）
     * 4. 所有变更同一事务
     */
    ProjectApprovalSubmitVO submit(ProjectApprovalSubmitDTO dto);

    /**
     * 立项分页查询（待审核 / 已驳回页签）
     * 返回三级树：项目 → 申报内容 → 农户/设施明细
     */
    PageVO<ApprovalVO> page(long pageNum, long pageSize, String tab, String projectName,
                            String projectCategory, String projectSource, String startTime, String endTime);

    /** 各页签数量统计（与分页查询共享筛选条件） */
    ApprovalCountVO counts(String projectName, String projectCategory, String projectSource,
                           String startTime, String endTime);

    /** 立项详情（含申报内容与明细树） */
    ApprovalVO detail(Long id);

    /** 查询立项附件列表（项目级 + 申报材料级） */
    List<ApprovalAttachmentVO> attachments(Long approvalId);

    /** 编辑立项（仅待审核状态，同步维护项目级立项资料附件） */
    void edit(ApprovalEditDTO dto);

    /** 审核通过（待审核 → 已通过） */
    void approve(Long id);

    /** 审核驳回（待审核 → 已驳回） */
    void reject(Long id);

    /** 退回（已驳回 → 待审核） */
    void returnToPending(Long id);
}

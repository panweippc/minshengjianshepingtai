package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.ApprovalEditDTO;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.ProjectApprovalAttachment;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 立项管理服务接口
 */
public interface ProjectApprovalService {

    /** 分页查询（树形数据，按页签过滤状态） */
    PageVO<ProjectApproval> page(long pageNum, long pageSize, String tab, String projectName,
                                 String projectCategory, String projectSource,
                                 LocalDate startTime, LocalDate endTime);

    /**
     * 按当前筛选条件统计各页签数量
     * @return Map 包含三个 key：all、pending、rejected
     */
    Map<String, Long> counts(String projectName, String projectCategory, String projectSource,
                             LocalDate startTime, LocalDate endTime);

    /** 详情（含树形申报内容与明细） */
    ProjectApproval detail(Long id);

    /** 查询立项附件列表 */
    List<ProjectApprovalAttachment> listAttachments(Long approvalId);

    /** 编辑（仅待审核状态可编辑，含附件差异更新） */
    void edit(ApprovalEditDTO dto);

    /** 审核通过（待审核 → 已通过） */
    void approve(Long id);

    /** 审核驳回（待审核 → 已驳回） */
    void reject(Long id);

    /** 退回（已驳回 → 待审核） */
    void returnBack(Long id);
}

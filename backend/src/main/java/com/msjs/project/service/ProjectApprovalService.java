package com.msjs.project.service;

import com.msjs.project.dto.ProjectApprovalSubmitDTO;
import com.msjs.project.vo.ProjectApprovalSubmitVO;

public interface ProjectApprovalService {
    /**
     * 提交立项申请：
     * 1. 保存立项主表 + declare + detail + attachment
     * 2. 把被选中的声明单下所有 approved 明细 audit_status 改成 established
     * 3. 重新计算父申报单状态（autoPromoteParentStatus 扩展支持 established）
     * 4. 所有变更同一事务
     */
    ProjectApprovalSubmitVO submit(ProjectApprovalSubmitDTO dto);
}

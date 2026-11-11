package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.ConstructionEditDTO;
import com.msjs.project.vo.ConstructionNodeVO;

/**
 * 施工管理服务
 */
public interface ProjectConstructionService {

    /**
     * 施工分页查询：仅查询审核通过的立项，返回项目 → 申报 → 明细三级树，
     * 并按三级明细的施工状态聚合各级进度
     */
    PageVO<ConstructionNodeVO> page(long pageNum, long pageSize, String projectCode, String projectName,
                                    String projectCategory, String projectType);

    /**
     * 项目施工详情：一级查看页使用，返回施工队信息、施工日志、附件及三级申报明细
     */
    ConstructionNodeVO detail(Long approvalId);

    /**
     * 编辑施工信息：一级维护施工单位/工期，三级维护施工状态/工期
     */
    void edit(ConstructionEditDTO dto);
}

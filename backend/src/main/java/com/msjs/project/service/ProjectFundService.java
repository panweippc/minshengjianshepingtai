package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.FundSaveDTO;
import com.msjs.project.entity.ProjectFund;
import com.msjs.project.vo.FundNodeVO;

import java.util.List;

/**
 * 资金管理服务接口
 * 数据来源：立项管理中审核通过的项目
 */
public interface ProjectFundService {

    /** 分页查询（树形：项目 → 申报内容 → 农户/设施明细） */
    PageVO<FundNodeVO> page(long pageNum, long pageSize, String projectName,
                            String projectCategory, String projectSource);

    /** 查询指定节点的资金拨付记录 */
    List<ProjectFund> listRecords(String nodeType, Long nodeId);

    /** 资金登记（一级）/ 下拨资金（二级、三级） */
    void save(FundSaveDTO dto);
}

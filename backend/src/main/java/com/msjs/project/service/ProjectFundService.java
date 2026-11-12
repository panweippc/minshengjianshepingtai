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

    /** 一级项目资金查看（含立项资料附件与资金流水），供施工管理只读复用 */
    FundNodeVO approvalDetail(Long approvalId);

    /** 二级事项申报资金查看（含申报材料附件与资金流水），供施工管理只读复用 */
    FundNodeVO declareDetail(Long declareId);

    /** 三级明细资金查看（含申报材料附件与资金流水），供施工管理只读复用 */
    FundNodeVO detail(Long detailId);

    /** 查询指定节点的资金拨付记录 */
    List<ProjectFund> listRecords(String nodeType, Long nodeId);

    /** 资金登记（一级）/ 下拨资金（二级、三级） */
    void save(FundSaveDTO dto);
}

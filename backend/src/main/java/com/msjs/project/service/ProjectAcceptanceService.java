package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.AcceptDetailDTO;
import com.msjs.project.dto.AcceptNodeDTO;
import com.msjs.project.dto.AcceptRecordUpdateDTO;
import com.msjs.project.dto.ArchiveDTO;
import com.msjs.project.vo.AcceptanceNodeVO;

/**
 * 验收管理服务接口
 * 数据来源：立项管理中审核通过的项目（accept_status 为 NONE / APPLYING）
 * 三级树形：项目 → 申报内容 → 农户/公共基础设施明细
 * 验收进度 = 已验收明细数 / 全部明细数 × 100%
 */
public interface ProjectAcceptanceService {

    /** 分页查询（返回审核通过、未归档的项目） */
    PageVO<AcceptanceNodeVO> page(long pageNum, long pageSize, String projectName,
                                  String projectCategory, String projectSource);

    /** 按立项查询验收详情（含各级验收记录，供档案管理查看复用） */
    AcceptanceNodeVO detail(Long approvalId);

    /** 施工管理发起验收（标记项目进入验收管理） */
    void apply(Long approvalId, String applyBy);

    /** 三级明细验收确认 */
    void acceptDetail(AcceptDetailDTO dto);

    /** 一级项目 / 二级申报验收确认（每级只维护自己的验收状态） */
    void acceptNode(AcceptNodeDTO dto);

    /** 编辑验收记录（修改结果/时间/人/备注/附件，状态随结果联动） */
    void updateRecord(AcceptRecordUpdateDTO dto);

    /** 项目归档（验收进度必须 100%） */
    void archive(ArchiveDTO dto);
}

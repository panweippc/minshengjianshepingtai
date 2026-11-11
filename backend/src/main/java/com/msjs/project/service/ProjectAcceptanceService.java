package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.AcceptDetailDTO;
import com.msjs.project.dto.ArchiveDTO;
import com.msjs.project.vo.AcceptanceNodeVO;

/**
 * 验收管理服务接口
 * 数据来源：施工管理中由"验收"按钮发起的项目（accept_status = APPLYING）
 * 三级树形：项目 → 申报内容 → 农户/公共基础设施明细
 * 验收进度 = 已验收明细数 / 全部明细数 × 100%
 */
public interface ProjectAcceptanceService {

    /** 分页查询（只返回已发起验收的项目） */
    PageVO<AcceptanceNodeVO> page(long pageNum, long pageSize, String projectName,
                                  String projectCategory, String projectSource);

    /** 施工管理发起验收（标记项目进入验收管理） */
    void apply(Long approvalId, String applyBy);

    /** 三级明细验收确认 */
    void acceptDetail(AcceptDetailDTO dto);

    /** 项目归档（验收进度必须 100%） */
    void archive(ArchiveDTO dto);
}

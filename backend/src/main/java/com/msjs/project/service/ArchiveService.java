package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.ArchiveEditDTO;
import com.msjs.project.vo.ArchiveDetailVO;
import com.msjs.project.vo.ArchiveVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 档案管理服务
 * 数据来源：验收管理归档后的项目（t_project_archive）
 * 档案查看聚合事项申报、立项、资金、施工、验收各环节数据
 */
public interface ArchiveService {

    /** 档案分页查询 */
    PageVO<ArchiveVO> page(long pageNum, long pageSize, String projectName,
                           String projectCategory, String projectType);

    /** 档案查看详情（六页签数据聚合） */
    ArchiveDetailVO detail(Long id);

    /** 维护档案位置信息（存储位置、归档时间），同步写入审计日志 */
    void update(ArchiveEditDTO dto);

    /** 导出项目全部资料压缩包 */
    void export(Long id, HttpServletResponse response);
}

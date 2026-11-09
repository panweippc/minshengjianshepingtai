package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.MaterialDamageSaveDTO;
import com.msjs.project.dto.MaterialHandoverSaveDTO;
import com.msjs.project.dto.MaterialInventorySaveDTO;
import com.msjs.project.vo.MaterialReportDetailVO;
import com.msjs.project.vo.MaterialReportVO;

import java.time.LocalDate;

/**
 * 物资统计报表服务
 * 支持盘点表(INVENTORY)、交接表(HANDOVER)、损毁表(DAMAGE)三种类型
 */
public interface MaterialReportService {

    /**
     * 统计报分页查询
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param reportName 报表名称（模糊）
     * @param reportType 报表类型（精确）
     * @param startTime  开始日期
     * @param endTime    结束日期
     */
    PageVO<MaterialReportVO> page(long pageNum, long pageSize, String reportName, String reportType,
                                  LocalDate startTime, LocalDate endTime);

    /** 统计报表详情（查看/编辑回显） */
    MaterialReportDetailVO detail(Long id);

    /** 新增盘点表（主表与明细同事务保存） */
    Long addInventory(MaterialInventorySaveDTO dto);

    /** 编辑盘点表（主表更新、明细整体覆盖，同事务） */
    void editInventory(MaterialInventorySaveDTO dto);

    /** 新增交接表 */
    Long addHandover(MaterialHandoverSaveDTO dto);

    /** 编辑交接表 */
    void editHandover(MaterialHandoverSaveDTO dto);

    /** 新增损毁表 */
    Long addDamage(MaterialDamageSaveDTO dto);

    /** 编辑损毁表 */
    void editDamage(MaterialDamageSaveDTO dto);

    /** 删除统计报表（主表与明细同事务逻辑删除） */
    void delete(Long id);
}

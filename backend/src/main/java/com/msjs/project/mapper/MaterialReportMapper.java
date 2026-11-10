package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.MaterialReport;
import com.msjs.project.vo.MaterialStockVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 物资统计报表主表 Mapper
 */
@Mapper
public interface MaterialReportMapper extends BaseMapper<MaterialReport> {

    /**
     * 按物品+库房汇总库存（入库合计 - 出库合计），用于盘点表自动带出库房与账面数量
     */
    @Select("""
        <script>
        SELECT s.warehouse_name AS warehouseName,
               s.item_name      AS itemName,
               MAX(i.unit)      AS unit,
               CAST(SUM(s.net_qty) AS SIGNED) AS bookQty
        FROM (
            SELECT warehouse_name, item_name, quantity AS net_qty
            FROM t_material_stock_in_item WHERE deleted = 0
            UNION ALL
            SELECT warehouse_name, item_name, -quantity AS net_qty
            FROM t_material_stock_out_item WHERE deleted = 0
        ) s
        LEFT JOIN t_material_item i ON i.item_name = s.item_name AND i.deleted = 0
        <where>
            <if test="itemName != null and itemName != ''">
                AND s.item_name LIKE CONCAT('%', #{itemName}, '%')
            </if>
            <if test="warehouseName != null and warehouseName != ''">
                AND s.warehouse_name = #{warehouseName}
            </if>
            <if test="itemCategory != null and itemCategory != ''">
                AND i.item_category = #{itemCategory}
            </if>
        </where>
        GROUP BY s.warehouse_name, s.item_name
        ORDER BY s.warehouse_name, s.item_name
        </script>
        """)
    List<MaterialStockVO> selectStockList(@Param("itemName") String itemName,
                                          @Param("warehouseName") String warehouseName,
                                          @Param("itemCategory") String itemCategory);
}

package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.MaterialItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 物品信息 Mapper
 */
@Mapper
public interface MaterialItemMapper extends BaseMapper<MaterialItem> {

    /**
     * 统计在库物资总数：全部未删除物品的库存数量之和
     *
     * @return 库存总数，无数据时返回 0
     */
    @Select("SELECT IFNULL(SUM(stock_qty), 0) FROM t_material_item WHERE deleted = 0")
    Long sumStockQty();

    /**
     * 原子增减库存，避免读-改-写并发导致库存丢失更新
     *
     * @param id    物品ID
     * @param delta 变动数量（入库为正、出库/回退为负）
     * @return 受影响行数
     */
    @Update("UPDATE t_material_item SET stock_qty = IFNULL(stock_qty, 0) + #{delta}, "
            + "update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int addStock(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子扣减库存，库存不足时不更新，由调用方提示业务异常
     *
     * @param id  物品ID
     * @param qty 出库数量（正数）
     * @return 受影响行数，0 表示库存不足或物品不存在
     */
    @Update("UPDATE t_material_item SET stock_qty = IFNULL(stock_qty, 0) - #{qty}, "
            + "update_time = NOW() WHERE id = #{id} AND deleted = 0 "
            + "AND IFNULL(stock_qty, 0) >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);
}

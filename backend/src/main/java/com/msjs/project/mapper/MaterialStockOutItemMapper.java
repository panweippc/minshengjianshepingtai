package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.MaterialStockOutItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库单明细 Mapper
 */
@Mapper
public interface MaterialStockOutItemMapper extends BaseMapper<MaterialStockOutItem> {
}

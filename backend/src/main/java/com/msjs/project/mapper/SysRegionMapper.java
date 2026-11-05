package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.SysRegion;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 行政区划 Mapper
 */
public interface SysRegionMapper extends BaseMapper<SysRegion> {

    /**
     * 统计区划编码占用数量（包含已逻辑删除的数据）
     * 唯一索引 uk_region_code 不受逻辑删除影响，新增前需按物理数据校验，避免重复键异常
     */
    @Select("SELECT COUNT(*) FROM t_sys_region WHERE code = #{code}")
    long countByCodeIncludeDeleted(@Param("code") String code);
}

package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.BaseFacility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 公共基础设施 Mapper
 */
@Mapper
public interface BaseFacilityMapper extends BaseMapper<BaseFacility> {

    /**
     * 按设施编号统计有效记录数（并发校验用）
     */
    @Select("SELECT COUNT(*) FROM t_base_facility WHERE facility_code = #{facilityCode} AND deleted = 0")
    long countActiveByCode(@Param("facilityCode") String facilityCode);
}

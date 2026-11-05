package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.Household;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 户基础信息 Mapper
 */
@Mapper
public interface HouseholdMapper extends BaseMapper<Household> {

    /**
     * 统计指定户号的记录数（包含已逻辑删除的数据）
     * 用于新增时校验户号是否已被占用，避免触发数据库唯一索引冲突
     */
    @Select("SELECT COUNT(1) FROM t_base_household WHERE household_no = #{householdNo}")
    long countByHouseholdNo(@Param("householdNo") String householdNo);
}

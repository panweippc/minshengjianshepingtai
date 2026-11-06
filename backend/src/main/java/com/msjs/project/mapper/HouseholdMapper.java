package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.Household;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 户基础信息 Mapper
 */
@Mapper
public interface HouseholdMapper extends BaseMapper<Household> {

    /**
     * 统计指定户号的正常记录数（仅未删除数据）
     * 用于新增时校验户号是否已被占用；已删除记录不占用户号，允许复用
     */
    @Select("SELECT COUNT(1) FROM t_base_household WHERE household_no = #{householdNo} AND deleted = 0")
    long countByHouseholdNo(@Param("householdNo") String householdNo);

    /**
     * 逻辑删除：deleted 写入本行ID（墓碑值，天然唯一）。
     * 配合 (household_no, deleted) 复合唯一索引，删除后该户号可被新记录复用
     */
    @Update("UPDATE t_base_household SET deleted = id WHERE id = #{id} AND deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}

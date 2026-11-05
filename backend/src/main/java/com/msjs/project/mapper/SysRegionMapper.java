package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.SysRegion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 行政区划 Mapper
 */
public interface SysRegionMapper extends BaseMapper<SysRegion> {

    /**
     * 统计有效区划编码数量（不含逻辑删除）
     */
    @Select("SELECT COUNT(*) FROM t_sys_region WHERE code = #{code} AND deleted = 0")
    long countActiveByCode(@Param("code") String code);

    /**
     * 物理清理已逻辑删除的同编码区划
     * 已删除区划不再占用编码，允许下次新增时复用相同编码
     */
    @Delete("DELETE FROM t_sys_region WHERE code = #{code} AND deleted = 1")
    int deleteDeletedByCode(@Param("code") String code);
}

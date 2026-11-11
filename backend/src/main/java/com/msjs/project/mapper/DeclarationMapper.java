package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.Declaration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 申报主表 Mapper
 */
@Mapper
public interface DeclarationMapper extends BaseMapper<Declaration> {

    /**
     * 统计指定申报编号的正常记录数（仅未删除数据）
     * 用于新增时校验申报编号是否已被占用；已删除记录不占用编号，允许复用
     */
    @Select("SELECT COUNT(1) FROM t_declaration WHERE business_code = #{businessCode} AND deleted = 0")
    long countByBusinessCode(@Param("businessCode") String businessCode);
}

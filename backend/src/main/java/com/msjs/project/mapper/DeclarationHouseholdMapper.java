package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.DeclarationHousehold;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 申报-农户明细 Mapper
 */
@Mapper
public interface DeclarationHouseholdMapper extends BaseMapper<DeclarationHousehold> {

    /**
     * 按申报单ID逻辑删除全部明细（更新主表时先清空再批量插入，保证快照与主表一致）
     */
    @Update("UPDATE t_declaration_household SET deleted = id WHERE declaration_id = #{declarationId} AND deleted = 0")
    int logicDeleteByDeclarationId(@Param("declarationId") Long declarationId);
}

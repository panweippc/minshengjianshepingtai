package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.DeclarationPublicFacility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 申报-公建设施明细 Mapper
 */
@Mapper
public interface DeclarationPublicFacilityMapper extends BaseMapper<DeclarationPublicFacility> {

    /**
     * 按申报单ID逻辑删除全部明细
     */
    @Update("UPDATE t_declaration_public_facility SET deleted = 1 WHERE declaration_id = #{declarationId} AND deleted = 0")
    int logicDeleteByDeclarationId(@Param("declarationId") Long declarationId);
}

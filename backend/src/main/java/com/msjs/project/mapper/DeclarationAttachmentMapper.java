package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.DeclarationAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 通用附件 Mapper
 */
@Mapper
public interface DeclarationAttachmentMapper extends BaseMapper<DeclarationAttachment> {

    /**
     * 按归属类型+归属ID 逻辑删除全部附件
     */
    @Update("UPDATE t_declaration_attachment SET deleted = 1 WHERE owner_type = #{ownerType} AND owner_id = #{ownerId} AND deleted = 0")
    int logicDeleteByOwner(@Param("ownerType") String ownerType, @Param("ownerId") Long ownerId);
}

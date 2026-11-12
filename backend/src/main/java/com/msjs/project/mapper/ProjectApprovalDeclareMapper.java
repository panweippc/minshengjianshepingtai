package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.ProjectApprovalDeclare;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProjectApprovalDeclareMapper extends BaseMapper<ProjectApprovalDeclare> {

    /**
     * 物理删除立项-申报关联行。
     * t_project_approval_declare 上 declare_code 有唯一索引，逻辑删除会残留占位导致同一申报单无法重新关联，
     * 因此解绑时必须物理删除。
     */
    @Delete("DELETE FROM t_project_approval_declare WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);

    /** 物理清理历史逻辑删除行，避免唯一索引占位 */
    @Delete("DELETE FROM t_project_approval_declare WHERE declare_code = #{declareCode} AND deleted = 1")
    int hardDeleteSoftDeletedByCode(@Param("declareCode") String declareCode);
}

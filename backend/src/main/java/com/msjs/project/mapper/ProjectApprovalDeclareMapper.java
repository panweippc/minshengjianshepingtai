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
     * 业务约束：解绑某申报单时，对应的 detail 与明细 audit_status 需要同步回退为 approved，
     * 因此直接物理删除，避免残留孤儿逻辑行。
     * 注意：declare_code / declaration_id 均为普通非唯一索引，一条申报单可同时挂在多条立项下，
     * 这里不再依赖唯一索引强制单申报单单立项。
     */
    @Delete("DELETE FROM t_project_approval_declare WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);

    /** 物理清理历史逻辑删除行（已无唯一索引占位担忧，但保留清理习惯） */
    @Delete("DELETE FROM t_project_approval_declare WHERE declare_code = #{declareCode} AND deleted = 1")
    int hardDeleteSoftDeletedByCode(@Param("declareCode") String declareCode);
}

package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统角色 Mapper
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 查询用户拥有的启用角色 */
    @Select("SELECT r.id, r.role_name, r.role_key, r.data_scope, r.status, r.remark, r.create_time, r.update_time, r.deleted " +
            "FROM t_sys_role r INNER JOIN t_sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}

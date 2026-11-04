package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.msjs.project.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统菜单 Mapper
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 查询用户可见的启用菜单（经角色-菜单关联去重） */
    @Select("SELECT DISTINCT m.id, m.parent_id, m.menu_name, m.menu_type, m.path, m.perm, m.icon, m.sort, m.status, m.create_time, m.update_time, m.deleted " +
            "FROM t_sys_menu m INNER JOIN t_sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN t_sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0 ORDER BY m.sort")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    /** 查询角色已分配的菜单ID集合 */
    @Select("SELECT menu_id FROM t_sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}

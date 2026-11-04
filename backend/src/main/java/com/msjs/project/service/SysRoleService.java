package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.RoleSaveDTO;
import com.msjs.project.entity.SysRole;

import java.util.List;

/**
 * 系统角色服务
 */
public interface SysRoleService {

    /** 分页查询 */
    PageVO<SysRole> page(long pageNum, long pageSize, String roleName);

    /** 全部启用角色（下拉用） */
    List<SysRole> listAll();

    /** 新增角色（同事务分配菜单权限） */
    Long add(RoleSaveDTO dto);

    /** 编辑角色（同事务更新菜单权限） */
    void edit(RoleSaveDTO dto);

    /** 删除角色（清理关联，已被使用则禁止） */
    void delete(Long id);

    /** 角色已分配菜单ID集合 */
    List<Long> menuIds(Long roleId);
}

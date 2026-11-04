package com.msjs.project.service;

import com.msjs.project.dto.MenuSaveDTO;
import com.msjs.project.entity.SysMenu;

import java.util.List;

/**
 * 系统菜单服务
 */
public interface SysMenuService {

    /** 全量菜单树（管理页面用） */
    List<SysMenu> tree();

    /** 新增菜单 */
    Long add(MenuSaveDTO dto);

    /** 编辑菜单 */
    void edit(MenuSaveDTO dto);

    /** 删除菜单（含子级则禁止） */
    void delete(Long id);
}

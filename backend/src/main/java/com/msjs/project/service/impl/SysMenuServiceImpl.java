package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.common.BizException;
import com.msjs.project.dto.MenuSaveDTO;
import com.msjs.project.entity.SysMenu;
import com.msjs.project.entity.SysRoleMenu;
import com.msjs.project.mapper.SysMenuMapper;
import com.msjs.project.mapper.SysRoleMenuMapper;
import com.msjs.project.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统菜单服务实现
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<SysMenu> tree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort));
        Map<Long, SysMenu> map = new LinkedHashMap<>();
        all.forEach(m -> map.put(m.getId(), m));
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu m : map.values()) {
            if (m.getParentId() == null || m.getParentId() == 0 || !map.containsKey(m.getParentId())) {
                roots.add(m);
            } else {
                SysMenu parent = map.get(m.getParentId());
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(m);
            }
        }
        return roots;
    }

    @Override
    public Long add(MenuSaveDTO dto) {
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(dto, menu);
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    public void edit(MenuSaveDTO dto) {
        SysMenu exist = menuMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("菜单不存在");
        }
        if (dto.getId().equals(dto.getParentId())) {
            throw new BizException("父菜单不能是自身");
        }
        BeanUtils.copyProperties(dto, exist);
        menuMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException("存在子菜单，不可删除");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
    }
}

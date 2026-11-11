package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.PageVO;
import com.msjs.project.dto.RoleSaveDTO;
import com.msjs.project.entity.SysRole;
import com.msjs.project.entity.SysRoleMenu;
import com.msjs.project.entity.SysUserRole;
import com.msjs.project.mapper.SysRoleMapper;
import com.msjs.project.mapper.SysRoleMenuMapper;
import com.msjs.project.mapper.SysUserRoleMapper;
import com.msjs.project.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色服务实现
 * 角色保存与权限分配同事务，保证一致性
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public PageVO<SysRole> page(long pageNum, long pageSize, String roleName) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(roleName != null && !roleName.isBlank(), SysRole::getRoleName, roleName)
                .orderByDesc(SysRole::getCreateTime);
        return PageVO.of(roleMapper.selectPage(page, wrapper));
    }

    @Override
    public List<SysRole> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1).orderByAsc(SysRole::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(RoleSaveDTO dto) {
        checkRoleKeyUnique(dto.getRoleKey(), null);
        SysRole role = new SysRole();
        fillRole(role, dto);
        roleMapper.insert(role);
        saveRoleMenus(role.getId(), dto.getMenuIds());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(RoleSaveDTO dto) {
        SysRole exist = roleMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("角色不存在");
        }
        if ("admin".equals(exist.getRoleKey())) {
            throw new BizException("内置超级管理员角色不可修改");
        }
        checkRoleKeyUnique(dto.getRoleKey(), dto.getId());
        fillRole(exist, dto);
        roleMapper.updateById(exist);
        saveRoleMenus(exist.getId(), dto.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole exist = roleMapper.selectById(id);
        if (exist == null) {
            throw new BizException("角色不存在");
        }
        if ("admin".equals(exist.getRoleKey())) {
            throw new BizException("内置超级管理员角色不可删除");
        }
        Long used = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (used != null && used > 0) {
            throw new BizException("角色已分配给用户，不可删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    @Override
    public List<Long> menuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole exist = roleMapper.selectById(roleId);
        if (exist == null) {
            throw new BizException("角色不存在");
        }
        // 内置超级管理员拥有全部权限，禁止通过分配权限改动，防止误清空导致系统不可用
        if ("admin".equals(exist.getRoleKey())) {
            throw new BizException("内置超级管理员角色无需分配权限");
        }
        // 删除旧关联后重建，与角色元数据保持事务一致，杜绝脏数据
        saveRoleMenus(roleId, menuIds);
    }

    private void fillRole(SysRole role, RoleSaveDTO dto) {
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setDataScope(dto.getDataScope());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setRemark(dto.getRemark());
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    private void checkRoleKeyUnique(String roleKey, Long excludeId) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleKey)
                .ne(excludeId != null, SysRole::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("角色标识已存在：" + roleKey);
        }
    }
}

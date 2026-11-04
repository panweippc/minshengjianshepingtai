package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.PageVO;
import com.msjs.project.dto.PasswordDTO;
import com.msjs.project.dto.UserSaveDTO;
import com.msjs.project.entity.SysUser;
import com.msjs.project.entity.SysUserRole;
import com.msjs.project.mapper.SysUserMapper;
import com.msjs.project.mapper.SysUserRoleMapper;
import com.msjs.project.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统用户服务实现
 * 用户保存与角色分配同事务，保证一致性，杜绝脏数据
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public PageVO<SysUser> page(long pageNum, long pageSize, String username, String realName) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        return PageVO.of(userMapper.selectUserPage(page,
                blankToNull(username), blankToNull(realName)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(UserSaveDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BizException("新增用户时密码不能为空");
        }
        checkUsernameUnique(dto.getUsername(), null);
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setRegionCode(dto.getRegionCode());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setRemark(dto.getRemark());
        userMapper.insert(user);
        saveUserRoles(user.getId(), dto.getRoleIds());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(UserSaveDTO dto) {
        SysUser exist = userMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        if ("admin".equals(exist.getUsername()) && !exist.getUsername().equals(dto.getUsername())) {
            throw new BizException("内置管理员账号不可改名");
        }
        checkUsernameUnique(dto.getUsername(), dto.getId());
        exist.setUsername(dto.getUsername());
        exist.setRealName(dto.getRealName());
        exist.setRegionCode(dto.getRegionCode());
        exist.setPhone(dto.getPhone());
        if (dto.getStatus() != null) {
            exist.setStatus(dto.getStatus());
        }
        exist.setRemark(dto.getRemark());
        // 密码留空表示不修改
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            exist.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userMapper.updateById(exist);
        saveUserRoles(exist.getId(), dto.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser exist = userMapper.selectById(id);
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        if ("admin".equals(exist.getUsername())) {
            throw new BizException("内置管理员账号不可删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Override
    public void resetPassword(PasswordDTO dto) {
        SysUser exist = userMapper.selectById(dto.getUserId());
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        exist.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(exist);
    }

    @Override
    public List<Long> roleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    /** 重建用户-角色关联 */
    private void saveUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("登录账号已存在：" + username);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}

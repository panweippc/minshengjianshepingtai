package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjs.project.common.BizException;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.ChangePasswordDTO;
import com.msjs.project.dto.LoginDTO;
import com.msjs.project.dto.ProfileUpdateDTO;
import com.msjs.project.entity.SysMenu;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.entity.SysRole;
import com.msjs.project.entity.SysUser;
import com.msjs.project.mapper.SysMenuMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.mapper.SysRoleMapper;
import com.msjs.project.mapper.SysUserMapper;
import com.msjs.project.service.AuthService;
import com.msjs.project.vo.LoginVO;
import com.msjs.project.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 登录鉴权服务实现
 * 令牌存储于 Redis，30分钟滑动过期；权限与角色随令牌缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** Redis 令牌键前缀 */
    private static final String TOKEN_KEY_PREFIX = "msjs:token:";
    /** 令牌有效期（分钟），滑动续期 */
    private static final long TOKEN_EXPIRE_MINUTES = 30;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRegionMapper regionMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginDTO dto, String ip) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已被停用，请联系管理员");
        }

        LoginUser loginUser = buildLoginUser(user);
        String token = UUID.randomUUID().toString().replace("-", "");
        cacheToken(token, loginUser);
        log.info("用户登录成功：{}，IP：{}", user.getUsername(), ip);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setRealName(user.getRealName());
        return vo;
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + token);
        }
    }

    @Override
    public LoginUser getLoginUser(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String key = TOKEN_KEY_PREFIX + token;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            LoginUser user = objectMapper.readValue(json, LoginUser.class);
            // 滑动续期
            redisTemplate.expire(key, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return user;
        } catch (Exception e) {
            log.warn("令牌解析失败", e);
            return null;
        }
    }

    @Override
    public UserInfoVO currentUserInfo() {
        LoginUser user = requireLoginUser();
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRegionCode(user.getRegionCode());
        vo.setRoles(user.getRoleKeys());
        vo.setPerms(user.getPerms());
        vo.setDataScope(user.getDataScope());

        // 姓名、手机号、状态、创建时间以数据库为准，避免个人资料修改后缓存与页面不一致
        SysUser current = userMapper.selectById(user.getUserId());
        if (current != null) {
            vo.setRealName(current.getRealName());
            vo.setPhone(current.getPhone());
            vo.setStatus(current.getStatus());
            vo.setCreateTime(current.getCreateTime());
        }

        if (user.getRegionCode() != null) {
            SysRegion region = regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                    .eq(SysRegion::getCode, user.getRegionCode()));
            vo.setRegionName(region == null ? null : region.getName());
        }

        // 可见菜单树：admin 全量，其余按角色关联
        List<SysMenu> menus;
        if (user.isAdmin()) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, 1).orderByAsc(SysMenu::getSort));
        } else {
            menus = menuMapper.selectMenusByUserId(user.getUserId());
        }
        // 前端路由只需目录与菜单
        vo.setMenus(buildTree(menus.stream()
                .filter(m -> !"F".equals(m.getMenuType()))
                .collect(Collectors.toList())));
        return vo;
    }

    @Override
    public void updateProfile(ProfileUpdateDTO dto) {
        LoginUser loginUser = requireLoginUser();
        // 使用 UpdateWrapper 显式 set，支持将联系电话清空为 null
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, loginUser.getUserId())
                .set(SysUser::getRealName, dto.getRealName())
                .set(SysUser::getPhone, dto.getPhone()));
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        LoginUser loginUser = requireLoginUser();
        SysUser exist = userMapper.selectById(loginUser.getUserId());
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), exist.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }
        exist.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(exist);
    }

    /** 获取当前登录用户，未登录抛出业务异常 */
    private LoginUser requireLoginUser() {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BizException(401, "未登录或登录已过期，请重新登录");
        }
        return user;
    }

    /** 构建登录用户：聚合角色、权限、数据范围 */
    private LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setRegionCode(user.getRegionCode());

        List<SysRole> roles = roleMapper.selectRolesByUserId(user.getId());
        Set<String> roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());
        loginUser.setRoleKeys(roleKeys);

        // 数据权限范围取最宽松（数值最小）
        Integer scope = roles.stream().map(SysRole::getDataScope)
                .filter(s -> s != null).min(Integer::compareTo).orElse(3);
        loginUser.setDataScope(scope);

        if (roleKeys.contains("admin")) {
            loginUser.setPerms(Set.of("*"));
        } else {
            List<SysMenu> menus = menuMapper.selectMenusByUserId(user.getId());
            Set<String> perms = menus.stream().map(SysMenu::getPerm)
                    .filter(p -> p != null && !p.isBlank()).collect(Collectors.toSet());
            loginUser.setPerms(perms.isEmpty() ? new HashSet<>() : perms);
        }
        return loginUser;
    }

    private void cacheToken(String token, LoginUser user) {
        try {
            redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token,
                    objectMapper.writeValueAsString(user), TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new BizException("令牌签发失败：" + e.getMessage());
        }
    }

    /** 菜单树组装 */
    private List<SysMenu> buildTree(List<SysMenu> menus) {
        Map<Long, SysMenu> map = new LinkedHashMap<>();
        menus.forEach(m -> map.put(m.getId(), m));
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
}

package com.msjs.project.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.entity.SysUser;
import com.msjs.project.entity.SysUserRole;
import com.msjs.project.mapper.SysUserMapper;
import com.msjs.project.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内置数据初始化
 * 首次启动时创建内置管理员 admin/admin123 并绑定超级管理员角色（密码经 BCrypt 加密，不落入 SQL 脚本）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitRunner implements ApplicationRunner {

    /** 内置超级管理员角色ID（与 V1 初始化脚本一致） */
    private static final long ADMIN_ROLE_ID = 1001L;

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "admin"));
        if (count != null && count > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRealName("系统管理员");
        admin.setRegionCode(null);
        admin.setStatus(1);
        admin.setRemark("内置管理员，首次登录后请修改密码");
        userMapper.insert(admin);

        SysUserRole ur = new SysUserRole();
        ur.setUserId(admin.getId());
        ur.setRoleId(ADMIN_ROLE_ID);
        userRoleMapper.insert(ur);
        log.info("内置管理员 admin 初始化完成（默认密码 admin123，请尽快修改）");
    }
}

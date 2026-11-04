package com.msjs.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.annotation.DataScope;
import com.msjs.project.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统用户 Mapper
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户（带行政区划行级数据隔离）
     * 数据权限过滤条件由 DataScopeInterceptor 依据当前登录用户自动追加
     */
    @DataScope(regionColumn = "u.region_code")
    @Select("SELECT u.id, u.username, u.real_name, u.region_code, u.phone, u.status, u.remark, u.create_time, u.update_time " +
            "FROM t_sys_user u WHERE u.deleted = 0 " +
            "AND (#{username} IS NULL OR u.username LIKE CONCAT('%', #{username}, '%')) " +
            "AND (#{realName} IS NULL OR u.real_name LIKE CONCAT('%', #{realName}, '%')) " +
            "ORDER BY u.create_time DESC")
    IPage<SysUser> selectUserPage(Page<SysUser> page, @Param("username") String username, @Param("realName") String realName);
}

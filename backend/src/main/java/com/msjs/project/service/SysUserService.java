package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.dto.PasswordDTO;
import com.msjs.project.dto.UserSaveDTO;
import com.msjs.project.entity.SysUser;

import java.util.List;

/**
 * 系统用户服务
 */
public interface SysUserService {

    /** 分页查询（自动套用行政区划数据权限） */
    PageVO<SysUser> page(long pageNum, long pageSize, String username, String realName);

    /** 新增用户（同事务分配角色） */
    Long add(UserSaveDTO dto);

    /** 编辑用户（同事务更新角色） */
    void edit(UserSaveDTO dto);

    /** 删除用户（同时清理角色关联） */
    void delete(Long id);

    /** 重置密码 */
    void resetPassword(PasswordDTO dto);

    /** 查询用户角色ID集合 */
    List<Long> roleIds(Long userId);
}

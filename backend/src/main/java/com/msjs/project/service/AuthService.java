package com.msjs.project.service;

import com.msjs.project.common.LoginUser;
import com.msjs.project.dto.LoginDTO;
import com.msjs.project.vo.LoginVO;
import com.msjs.project.vo.UserInfoVO;

/**
 * 登录鉴权服务
 */
public interface AuthService {

    /** 账号密码登录，成功签发令牌 */
    LoginVO login(LoginDTO dto, String ip);

    /** 退出登录，销毁令牌 */
    void logout(String token);

    /** 依据令牌获取登录用户（含刷新有效期） */
    LoginUser getLoginUser(String token);

    /** 当前登录用户完整信息（权限、菜单树） */
    UserInfoVO currentUserInfo();
}

package com.msjs.project.controller;

import com.msjs.project.common.Constants;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.dto.ChangePasswordDTO;
import com.msjs.project.dto.LoginDTO;
import com.msjs.project.dto.ProfileUpdateDTO;
import com.msjs.project.service.AuthService;
import com.msjs.project.vo.LoginVO;
import com.msjs.project.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录鉴权接口
 */
@Tag(name = "登录鉴权")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "账号密码登录")
    @OperLog(module = "登录鉴权", operation = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.ok(authService.login(dto, request.getRemoteAddr()));
    }

    @Operation(summary = "退出登录")
    @OperLog(module = "登录鉴权", operation = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(resolveToken(request));
        return Result.ok();
    }

    @Operation(summary = "当前登录用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(authService.currentUserInfo());
    }

    @Operation(summary = "修改个人资料")
    @OperLog(module = "个人中心", operation = "修改个人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        authService.updateProfile(dto);
        return Result.ok();
    }

    @Operation(summary = "修改登录密码")
    @OperLog(module = "个人中心", operation = "修改登录密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.ok();
    }

    /** 从请求头解析 Bearer 令牌 */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(Constants.HEADER_TOKEN);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }
}

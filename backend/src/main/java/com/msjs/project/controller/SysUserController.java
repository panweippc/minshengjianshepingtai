package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.PasswordDTO;
import com.msjs.project.dto.UserSaveDTO;
import com.msjs.project.entity.SysUser;
import com.msjs.project.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理接口
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "用户分页查询")
    @RequirePerm("system:user:list")
    @GetMapping("/page")
    public Result<PageVO<SysUser>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "登录账号") @RequestParam(required = false) String username,
            @Parameter(description = "姓名") @RequestParam(required = false) String realName) {
        return Result.ok(userService.page(pageNum, pageSize, username, realName));
    }

    @Operation(summary = "新增用户")
    @RequirePerm("system:user:add")
    @OperLog(module = "用户管理", operation = "新增用户")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody UserSaveDTO dto) {
        return Result.ok(userService.add(dto));
    }

    @Operation(summary = "编辑用户")
    @RequirePerm("system:user:edit")
    @OperLog(module = "用户管理", operation = "编辑用户")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody UserSaveDTO dto) {
        userService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @RequirePerm("system:user:delete")
    @OperLog(module = "用户管理", operation = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "重置密码")
    @RequirePerm("system:user:edit")
    @OperLog(module = "用户管理", operation = "重置密码")
    @PutMapping("/password")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordDTO dto) {
        userService.resetPassword(dto);
        return Result.ok();
    }

    @Operation(summary = "查询用户角色ID集合")
    @RequirePerm("system:user:list")
    @GetMapping("/{id}/roleIds")
    public Result<List<Long>> roleIds(@PathVariable("id") Long id) {
        return Result.ok(userService.roleIds(id));
    }
}

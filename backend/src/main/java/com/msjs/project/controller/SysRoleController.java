package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.RoleSaveDTO;
import com.msjs.project.entity.SysRole;
import com.msjs.project.service.SysRoleService;
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
 * 角色管理接口
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "角色分页查询")
    @RequirePerm("system:role:list")
    @GetMapping("/page")
    public Result<PageVO<SysRole>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "角色名称") @RequestParam(required = false) String roleName) {
        return Result.ok(roleService.page(pageNum, pageSize, roleName));
    }

    @Operation(summary = "全部启用角色（下拉）")
    @GetMapping("/all")
    public Result<List<SysRole>> all() {
        return Result.ok(roleService.listAll());
    }

    @Operation(summary = "新增角色")
    @RequirePerm("system:role:add")
    @OperLog(module = "角色管理", operation = "新增角色")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody RoleSaveDTO dto) {
        return Result.ok(roleService.add(dto));
    }

    @Operation(summary = "编辑角色")
    @RequirePerm("system:role:edit")
    @OperLog(module = "角色管理", operation = "编辑角色")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody RoleSaveDTO dto) {
        roleService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @RequirePerm("system:role:delete")
    @OperLog(module = "角色管理", operation = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "角色已分配菜单ID集合")
    @RequirePerm("system:role:list")
    @GetMapping("/{id}/menuIds")
    public Result<List<Long>> menuIds(@PathVariable("id") Long id) {
        return Result.ok(roleService.menuIds(id));
    }
}

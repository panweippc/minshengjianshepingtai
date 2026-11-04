package com.msjs.project.controller;

import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.MenuSaveDTO;
import com.msjs.project.entity.SysMenu;
import com.msjs.project.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理接口
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "菜单树")
    @RequirePerm("system:menu:list")
    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        return Result.ok(menuService.tree());
    }

    @Operation(summary = "新增菜单")
    @RequirePerm("system:menu:add")
    @OperLog(module = "菜单管理", operation = "新增菜单")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody MenuSaveDTO dto) {
        return Result.ok(menuService.add(dto));
    }

    @Operation(summary = "编辑菜单")
    @RequirePerm("system:menu:edit")
    @OperLog(module = "菜单管理", operation = "编辑菜单")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody MenuSaveDTO dto) {
        menuService.edit(dto);
        return Result.ok();
    }

    @Operation(summary = "删除菜单")
    @RequirePerm("system:menu:delete")
    @OperLog(module = "菜单管理", operation = "删除菜单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}

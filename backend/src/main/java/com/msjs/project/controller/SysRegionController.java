package com.msjs.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.Result;
import com.msjs.project.common.UserContext;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.mapper.SysRegionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 行政区划接口（用户表单下拉用）
 */
@Tag(name = "行政区划")
@RestController
@RequestMapping("/api/system/region")
@RequiredArgsConstructor
public class SysRegionController {

    private final SysRegionMapper regionMapper;

    /**
     * 行政区划列表（带数据权限过滤）。
     * 权限规则：
     *   - 未登录 / 未绑定区划：返回全部区划（用于系统管理员维护场景，不常见）
     *   - 镇级账号：仅返回当前镇 + 其直接下级村（level <= 5 且 code=自身 或 parentCode=自身镇）
     *   - 村级账号：仅返回当前村 + 其直接下级组
     *   - 组级账号：仅返回当前组自身
     * 备注：本接口用于下拉选择，返回的仅为"当前账号可见范围"内的区划，禁止越权跨镇/跨村查看。
     */
    @Operation(summary = "行政区划列表")
    @GetMapping("/list")
    public Result<List<SysRegion>> list() {
        LoginUser loginUser = UserContext.get();
        String userRegionCode = loginUser == null ? null : loginUser.getRegionCode();
        if (!StringUtils.hasText(userRegionCode)) {
            // 兜底：未绑定区划的账号返回全量
            return Result.ok(regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                    .orderByAsc(SysRegion::getSort)));
        }

        SysRegion userRegion = regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getCode, userRegionCode.trim()));
        if (userRegion == null) {
            return Result.ok(Collections.emptyList());
        }

        // 递归收集当前用户区划 + 其所有下级编码，保证多级嵌套场景下村级账号仍能看到村下所有组
        List<String> scopeCodes = new ArrayList<>();
        scopeCodes.add(userRegion.getCode());
        collectDescendantCodes(userRegion.getCode(), scopeCodes);

        return Result.ok(regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .in(SysRegion::getCode, scopeCodes)
                .orderByAsc(SysRegion::getSort)));
    }

    /**
     * 按 parentCode 筛选子区划（用于前端做镇→村、村→组 联动）。
     * 同 list() 一样带数据权限：parentCode 必须在当前用户权限范围内才允许查。
     */
    @Operation(summary = "下级区划列表")
    @GetMapping("/children")
    public Result<List<SysRegion>> children(@RequestParam String parentCode) {
        if (!StringUtils.hasText(parentCode)) {
            return Result.ok(Collections.emptyList());
        }
        LoginUser loginUser = UserContext.get();
        String userRegionCode = loginUser == null ? null : loginUser.getRegionCode();

        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getParentCode, parentCode.trim());
        wrapper.orderByAsc(SysRegion::getSort);

        if (StringUtils.hasText(userRegionCode)) {
            // 确保 parentCode 在用户可见范围内（禁止跨镇/跨村越级拉数据）
            List<String> scopeCodes = new ArrayList<>();
            scopeCodes.add(userRegionCode.trim());
            collectDescendantCodes(userRegionCode.trim(), scopeCodes);
            if (!scopeCodes.contains(parentCode.trim())) {
                return Result.ok(Collections.emptyList());
            }
            wrapper.in(SysRegion::getCode, scopeCodes);
        }
        return Result.ok(regionMapper.selectList(wrapper));
    }

    /** 递归把 parentCode 的所有下级塞进 scopeCodes */
    private void collectDescendantCodes(String parentCode, List<String> scopeCodes) {
        List<SysRegion> children = regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getParentCode, parentCode));
        if (children == null || children.isEmpty()) {
            return;
        }
        for (SysRegion child : children) {
            scopeCodes.add(child.getCode());
            collectDescendantCodes(child.getCode(), scopeCodes);
        }
    }
}

package com.msjs.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.common.Result;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.mapper.SysRegionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(summary = "行政区划列表")
    @GetMapping("/list")
    public Result<List<SysRegion>> list() {
        return Result.ok(regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .orderByAsc(SysRegion::getSort)));
    }
}

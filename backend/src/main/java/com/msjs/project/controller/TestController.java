package com.msjs.project.controller;

import com.msjs.project.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 联通性测试接口
 */
@Tag(name = "系统测试")
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "msjs-backend");
        data.put("time", LocalDateTime.now().toString());
        return Result.ok(data);
    }
}

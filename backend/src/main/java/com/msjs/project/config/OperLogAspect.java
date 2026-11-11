package com.msjs.project.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.Result;
import com.msjs.project.common.UserContext;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.entity.SysOperLog;
import com.msjs.project.service.SysOperLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志切面
 * 全量留存 /api 下所有控制器调用：操作人、IP、时间、参数、结果、耗时，全程可追溯
 * 业务约束：参数中的密码字段不落地；文件流不序列化；参数截断2000字符
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    /** 参数最大留存长度 */
    private static final int PARAM_MAX_LEN = 2000;

    /** HTTP请求方式 → 中文操作语义（RESTful规范：GET查询 POST新增 PUT修改 DELETE删除） */
    private static final Map<String, String> METHOD_OPERATION_MAP = Map.of(
            "GET", "查询",
            "POST", "新增",
            "PUT", "修改",
            "DELETE", "删除"
    );

    private final SysOperLogService operLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("execution(* com.msjs.project.controller..*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                saveLog(joinPoint, result, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                // 日志写入失败不影响业务
                log.warn("操作日志写入失败", e);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, Object result, Throwable error, long costMs) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();

        SysOperLog operLog = new SysOperLog();
        LoginUser user = UserContext.get();
        if (user != null) {
            operLog.setUserId(user.getUserId());
            operLog.setUsername(user.getUsername());
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        OperLog anno = signature.getMethod().getAnnotation(OperLog.class);
        // 模块兜底优先级：@OperLog注解 > Controller类上的@Tag中文注解 > 类名
        Class<?> controllerClass = signature.getDeclaringType();
        Tag tag = controllerClass.getAnnotation(Tag.class);
        if (anno != null && !anno.module().isBlank()) {
            operLog.setModule(anno.module());
        } else if (tag != null && !tag.name().isBlank()) {
            operLog.setModule(tag.name());
        } else {
            operLog.setModule(controllerClass.getSimpleName());
        }
        // 操作兜底：@OperLog注解 > HTTP请求方式中文映射，避免日志出现英文方法名
        if (anno != null && !anno.operation().isBlank()) {
            operLog.setOperation(anno.operation());
        } else {
            operLog.setOperation(METHOD_OPERATION_MAP.getOrDefault(request.getMethod(), request.getMethod()));
        }
        operLog.setMethod(request.getMethod());
        operLog.setUrl(request.getRequestURI());
        operLog.setIp(request.getRemoteAddr());
        operLog.setParams(buildParams(joinPoint));
        if (error != null) {
            operLog.setResultCode(500);
            operLog.setErrorMsg(truncate(error.getMessage()));
        } else if (result instanceof Result<?> r) {
            operLog.setResultCode(r.getCode());
        } else {
            operLog.setResultCode(200);
        }
        operLog.setOperTime(LocalDateTime.now());
        operLog.setCostMs(costMs);
        operLogService.save(operLog);
    }

    /** 序列化请求参数：剔除文件流与敏感字段，截断存储 */
    private String buildParams(ProceedingJoinPoint joinPoint) {
        try {
            String json = Arrays.stream(joinPoint.getArgs())
                    .filter(a -> !(a instanceof MultipartFile) && !(a instanceof HttpServletRequest))
                    .map(a -> {
                        try {
                            return objectMapper.writeValueAsString(a);
                        } catch (Exception e) {
                            return String.valueOf(a);
                        }
                    })
                    .collect(Collectors.joining(","));
            // 密码不落日志
            json = json.replaceAll("\"(password|newPassword)\":\"[^\"]*\"", "\"$1\":\"******\"");
            return truncate(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > PARAM_MAX_LEN ? s.substring(0, PARAM_MAX_LEN) : s;
    }
}

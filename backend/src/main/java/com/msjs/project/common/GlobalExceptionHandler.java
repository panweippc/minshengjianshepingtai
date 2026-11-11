package com.msjs.project.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局统一异常捕获
 * 原始堆栈信息不返回前端，仅记录服务端日志
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    /** JSR380 参数校验异常（@RequestBody） */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", msg);
        return Result.fail(msg);
    }

    /** JSR380 参数校验异常（@RequestParam/@PathVariable） */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("参数校验失败：{}", e.getMessage());
        return Result.fail("参数校验失败：" + e.getMessage());
    }

    /** 缺少请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail("缺少必填参数：" + e.getParameterName());
    }

    /** 兜底异常：不向前端暴露堆栈（开发阶段额外透出原始 msg 便于排查） */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        String rawMsg = e.getMessage();
        if (rawMsg != null && !rawMsg.isBlank()) {
            return Result.fail("系统繁忙：" + rawMsg);
        }
        return Result.fail("系统繁忙，请稍后重试");
    }
}

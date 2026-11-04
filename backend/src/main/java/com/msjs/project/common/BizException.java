package com.msjs.project.common;

import lombok.Getter;

/**
 * 业务异常
 * 业务校验失败统一抛出此异常，由全局异常处理器捕获返回前端友好提示
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = Constants.FAIL_CODE;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}

package com.msjs.project.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结构体
 * {code:200/500, msg:"提示", data:{} | [], success:boolean}
 */
@Data
@Schema(description = "统一返回结构")
public class Result<T> implements Serializable {

    @Schema(description = "状态码，200成功，500失败")
    private Integer code;

    @Schema(description = "提示信息")
    private String msg;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "是否成功")
    private Boolean success;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(Constants.SUCCESS_CODE);
        r.setMsg("操作成功");
        r.setData(data);
        r.setSuccess(true);
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> r = new Result<>();
        r.setCode(Constants.FAIL_CODE);
        r.setMsg(msg);
        r.setSuccess(false);
        return r;
    }
}

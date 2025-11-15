package com.dzy666.demo.util;

import lombok.Data;
import java.io.Serializable;

@Data
public class JsonResult<T> implements Serializable {
    private Boolean success;
    private String message;
    private T data;
    private Integer code;

    // 私有构造方法
    private JsonResult() {}

    // 成功静态方法
    public static <T> JsonResult<T> success() {
        JsonResult<T> result = new JsonResult<>();
        result.setSuccess(true);
        result.setMessage("操作成功");
        result.setCode(200);
        return result;
    }

    public static <T> JsonResult<T> success(T data) {
        JsonResult<T> result = new JsonResult<>();
        result.setSuccess(true);
        result.setMessage("操作成功");
        result.setData(data);
        result.setCode(200);
        return result;
    }

    public static <T> JsonResult<T> success(String message, T data) {
        JsonResult<T> result = new JsonResult<>();
        result.setSuccess(true);
        result.setMessage(message);
        result.setData(data);
        result.setCode(200);
        return result;
    }

    // 失败静态方法
    public static <T> JsonResult<T> error(String message) {
        JsonResult<T> result = new JsonResult<>();
        result.setSuccess(false);
        result.setMessage(message);
        result.setCode(500);
        return result;
    }

    public static <T> JsonResult<T> error(Integer code, String message) {
        JsonResult<T> result = new JsonResult<>();
        result.setSuccess(false);
        result.setMessage(message);
        result.setCode(code);
        return result;
    }

    public static <T> JsonResult<T> error(Integer code, String message, T data) {
        JsonResult<T> result = new JsonResult<>();
        result.setSuccess(false);
        result.setMessage(message);
        result.setCode(code);
        result.setData(data);
        return result;
    }

    // 便捷方法
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}
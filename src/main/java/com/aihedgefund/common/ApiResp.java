package com.aihedgefund.common;

/**
 * 统一响应封装（内部使用，对外接口直接返回数据以兼容 Python FastAPI 格式）
 */
public class ApiResp<T> {
    private int code;
    private String msg;
    private T data;

    public ApiResp(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ApiResp<T> ok(T data) {
        return new ApiResp<T>(0, "ok", data);
    }

    public static <T> ApiResp<T> fail(String msg) {
        return new ApiResp<T>(-1, msg, null);
    }

    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public T getData() { return data; }
}

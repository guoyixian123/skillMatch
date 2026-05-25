package com.skillmatch.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    PARAM_ERROR(400, "参数错误"),
    NOT_LOGIN(401, "未登录"),
    NOT_AUTH(403, "未授权"),
    NOT_FOUND(404, "资源不存在"),
    EXCEED_LIMIT(429, "操作过于频繁"),
    SERVER_ERROR(500, "服务器异常");

    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}

package com.skillmatch.exceptions;

import com.skillmatch.enums.ErrorCode;

public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ErrorCode error) {
        super(error.getMessage());
        this.code = error.getCode();
    }
    // 自定义异常信息
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}

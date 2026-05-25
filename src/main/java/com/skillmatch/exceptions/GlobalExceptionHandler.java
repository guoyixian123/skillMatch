package com.skillmatch.exceptions;

import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.enums.ErrorCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler{
    /**
     * 捕获业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public RESTful<Object> handleException(BusinessException b) {

        return RESTful.error(
                b.getCode(),
                b.getMessage()
        );
    }
    /**
    * 捕获未知异常
     */
    @ExceptionHandler(Exception.class)
    public RESTful<Object> handleException(Exception e) {
        return RESTful.error(
                ErrorCode.SERVER_ERROR.getCode(),
                e.getMessage()
        );
    }
}

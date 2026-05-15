package com.skillmatch.domain.vo;

import lombok.Data;

@Data
public class RESTful<T> {
    private Integer code;
    private String message;
    private T data;
    public RESTful(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    public static <T> RESTful<T> success(T data) {
        return new RESTful<T>(200, "success", data);
    }
    public static <T> RESTful<T> error(Integer code, String message) {
        return new RESTful<T>(500, message, null);
    }
}

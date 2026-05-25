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
    /*
    * 成功返回结果
     */
    public static <T> RESTful<T> success(T data) {return new RESTful<>(200, "success", data);}

    public static <T> RESTful<T> success(T data, String message) {return new RESTful<>(200, message, data);}

    public static <T> RESTful<T> success() {return new RESTful<>(200, "success", null);}

    /*
    * 失败返回结果
     */
    public static <T> RESTful<T> error(int code,String message) {
        return new RESTful<>(code, message, null);
    }
}

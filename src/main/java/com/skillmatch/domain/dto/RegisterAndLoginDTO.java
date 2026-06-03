package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterAndLoginDTO {
    @NotBlank(message = "账号不能为空")
    @Size(min = 2, max = 32, message = "账号长度2-32")
    private String userId;
    @Size(min = 1, max = 16, message = "昵称1-16位")
    private String name;
    @Size(min = 6, max = 10, message = "密码6-10位")
    private String password;
}

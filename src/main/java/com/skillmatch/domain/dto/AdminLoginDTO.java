package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginDTO {

    @NotBlank(message = "账号不能为空")
    private String userId;

    @NotBlank(message = "密码不能为空")
    private String password;
}

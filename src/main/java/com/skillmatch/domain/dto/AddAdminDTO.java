package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 添加子管理员请求
 */
@Data
public class AddAdminDTO {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "管理员姓名不能为空")
    private String name;

    private String password;
}

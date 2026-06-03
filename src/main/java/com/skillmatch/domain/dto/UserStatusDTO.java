package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 冻结/解冻用户
 */
@Data
public class UserStatusDTO {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotNull(message = "状态不能为空")
    private Integer status;
}

package com.skillmatch.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeDTO {
    @NotBlank(message = "目标ID不能为空")
    private String bizId;
    @NotNull(message = "点赞类型不能为空")
    @Min(value = 1, message = "类型只能为1或2")
    @Max(value = 2, message = "类型只能为1或2")
    private Integer type;
}

package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除
 */
@Data
public class BatchDeleteDTO {

    @NotEmpty(message = "用户ID列表不能为空")
    private List<String> userIds;
}

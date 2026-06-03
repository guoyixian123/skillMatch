package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NotificationDTO {
    @NotBlank(message = "接收者不能为空")
    private String toUserId;
    @NotBlank(message = "请求理由不能为空")
    @Size(min = 1, max = 150, message = "理由1-150字")
    private String reason;
}

package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageDTO {
    @NotBlank(message = "接收者不能为空")
    private String toUserId;
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息最长2000字")
    private String content;
}

package com.skillmatch.domain.dto;

import lombok.Data;

@Data
public class SendMessageDTO {
    private String toUserId;
    private String content;
}

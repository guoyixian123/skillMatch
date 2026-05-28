package com.skillmatch.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {
    private String id;
    private String fromUserId;
    private String toUserId;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

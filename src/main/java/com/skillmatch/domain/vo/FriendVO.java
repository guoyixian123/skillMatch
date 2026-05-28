package com.skillmatch.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendVO {
    private String userId;
    private String name;
    private String avatarUrl;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
}

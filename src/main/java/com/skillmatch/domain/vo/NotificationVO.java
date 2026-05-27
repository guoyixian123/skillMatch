package com.skillmatch.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class NotificationVO {
    private String id;
    private UserBasicVO fromUser;
    private String reason;
    private byte status;//1:待处理 2:已经同意 3:已经拒绝
    private LocalDateTime createAt;
}

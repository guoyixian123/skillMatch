package com.skillmatch.domain.dto;

import lombok.Data;

@Data
public class NotificationDTO {
    private String toUserId;
    private String reason;
}

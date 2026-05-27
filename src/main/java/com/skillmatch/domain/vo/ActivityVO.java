package com.skillmatch.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ActivityVO {
    private String type;
    private String content;
    private LocalDateTime createdAt;
}

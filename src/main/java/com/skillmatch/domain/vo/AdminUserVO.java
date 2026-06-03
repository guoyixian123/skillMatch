package com.skillmatch.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理端用户详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserVO extends UserVO {

    private Double latitude;
    private Double longitude;
    private String city;
    private Boolean robot;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime updatedAt;

    /** 技能标签（逗号分隔） */
    private String skillTags;

    /** 兴趣标签（逗号分隔） */
    private String hobbyTags;
}

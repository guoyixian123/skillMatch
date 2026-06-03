package com.skillmatch.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员信息（管理端展示用）
 */
@Data
public class AdminInfoVO {
    private Long id;
    private String userId;
    private String name;
    private String role;       // ROOT / ADMIN
    private Long parentId;
    private String parentName; // 上级管理员姓名
    private Integer status;    // 1=正常 0=禁用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

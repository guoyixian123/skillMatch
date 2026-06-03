package com.skillmatch.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员表（独立于 user 表，支持层级关系）
 */
@Data
@TableName("admin_user")
public class AdminUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联 user 表 */
    private String userId;

    /** 管理员姓名 */
    private String name;

    /** 登录密码（MD5） */
    private String password;

    /** ROOT=根管理员（可添加子管理员） ADMIN=普通管理员 */
    private String role;

    /** 上级管理员ID（谁添加的），NULL=根管理员 */
    private Long parentId;

    /** 1=正常 0=禁用 */
    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.skillmatch.domain.po;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 *
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户主键,账号
     */
    @TableId(value = "user_id", type = IdType.ASSIGN_UUID)
    private String userId;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 账号密码
     */
    private String password;

    /**
     * 联系方式,如微信号,qq号
     */
    private String contactInfo;

    /**
     * 头像url
     */
    private String headUrl;

    /**
     * 签名
     */
    private String signature;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 注册时间
     */
    private LocalDateTime createdAt;

    /**
     * 发帖数
     */
    private String postCount;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;


}

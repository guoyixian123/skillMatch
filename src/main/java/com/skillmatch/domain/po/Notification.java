package com.skillmatch.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用通知表实体
 * 记录点赞等操作产生的通知，与技能交换请求（contact_request）分开存储
 *
 * @author Speed
 * @since 2026-05-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通知ID，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 接收通知的用户ID（被点赞者） */
    private String receiverId;

    /** 触发通知的用户ID（点赞者） */
    private String actorId;

    /** 通知类型：1=个人主页被赞，2=帖子被赞 */
    private Integer type;

    /** 关联的业务实体ID，type=2时存帖子ID */
    private String bizId;

    /** 是否已读：false=未读，true=已读 */
    private Boolean isRead;

    /** 通知创建时间 */
    private LocalDateTime createdAt;
}

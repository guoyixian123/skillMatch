package com.skillmatch.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞通知视图对象，返回给前端渲染通知列表
 *
 * @author Speed
 * @since 2026-05-27
 */
@Data
public class LikeNotificationVO {

    /** 通知ID */
    private Long id;

    /** 通知类型：1=个人主页被赞，2=帖子被赞 */
    private Integer type;

    /** 点赞者基本信息（id + 姓名 + 头像） */
    private UserBasicVO actor;

    /** 关联实体ID，type=2时为帖子ID，前端点通知可跳转到 /community/{bizId} */
    private String bizId;

    /** 是否已读 */
    private Boolean isRead;

    /** 摘要文案，后端组装好，前端直接展示，如"赞了你"、"赞了你的帖子《xxx》" */
    private String preview;

    /** 通知时间 */
    private LocalDateTime createdAt;
}

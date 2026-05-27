package com.skillmatch.service;

import com.skillmatch.domain.vo.LikeNotificationVO;
import com.skillmatch.domain.vo.PageVO;

/**
 * 通知服务接口
 *
 * @author Speed
 * @since 2026-05-27
 */
public interface INotificationService {

    /**
     * 保存一条通知记录
     *
     * @param actorId    触发者用户ID（点赞的人）
     * @param receiverId 接收者用户ID（被点赞的人）
     * @param type       通知类型：1=个人主页被赞，2=帖子被赞
     * @param bizId      关联实体ID，type=2时为帖子ID
     */
    void save(String actorId, String receiverId, Integer type, String bizId);

    /**
     * 取消点赞时删除对应的通知记录
     *
     * @param actorId 点赞者ID
     * @param bizId   被点赞实体ID
     * @param type    点赞类型
     */
    void removeByLike(String actorId, String bizId, Integer type);

    /**
     * 分页查询当前用户的通知列表，按时间倒序
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页通知列表，每条包含 actor 信息和 preview 文案
     */
    PageVO<LikeNotificationVO> list(int page, int size);

    /**
     * 获取当前用户的未读通知数量，用于前端铃铛角标
     *
     * @return 未读数量
     */
    Integer unreadCount();

    /**
     * 标记单条通知为已读，需校验通知属于当前用户
     *
     * @param id 通知ID
     */
    void markRead(Long id);

    /**
     * 一键标记当前用户所有未读通知为已读
     */
    void markAllRead();
}

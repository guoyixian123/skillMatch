package com.skillmatch.controller;

import com.skillmatch.domain.query.NotificationQuery;
import com.skillmatch.domain.vo.LikeNotificationVO;
import com.skillmatch.domain.vo.PageVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 点赞通知控制器
 * 提供通知列表查询、未读计数、标记已读等接口
 *
 * @author Speed
 * @since 2026-05-27
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class LikeNotificationController {

    private final INotificationService notificationService;

    /**
     * 获取点赞通知分页列表
     */
    @GetMapping
    public RESTful<PageVO<LikeNotificationVO>> list(NotificationQuery query) {
        PageVO<LikeNotificationVO> page = notificationService.list(query.getPage(), query.getSize());
        return RESTful.success(page);
    }

    /**
     * 获取未读通知数量，用于前端铃铛角标
     */
    @GetMapping("/unread-count")
    public RESTful<Integer> unreadCount() {
        return RESTful.success(notificationService.unreadCount());
    }

    /**
     * 标记单条通知为已读
     */
    @PutMapping("/{id}/read")
    public RESTful<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return RESTful.success(null, "已标记已读");
    }

    /**
     * 一键标记所有通知为已读
     */
    @PutMapping("/read-all")
    public RESTful<Void> markAllRead() {
        notificationService.markAllRead();
        return RESTful.success(null, "全部已读");
    }
}

package com.skillmatch.controller;


import com.skillmatch.domain.dto.NotificationDTO;
import com.skillmatch.domain.vo.NotificationVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.IContactRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Speed
 * @since 2026-05-25
 */
@Slf4j
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class ContactRequestController {
    private final IContactRequestService contactRequestService;
    @GetMapping("/requests/received")
    public RESTful<List<NotificationVO>> getReceivedRequests(@RequestParam(required = false) Integer status) {
        log.info("收到交换请求");
        List<NotificationVO> vo=contactRequestService.getNotification(status);
        return RESTful.success(vo);
    }
    @GetMapping("/requests/sent")
    public RESTful<List<NotificationVO>> getSentRequests(@RequestParam(required = false) Integer status) {
        log.info("获取已发送的交换请求");
        List<NotificationVO> vo=contactRequestService.getSentNotification(status);
        return RESTful.success(vo);
    }
    @PostMapping("/requests")
    public RESTful<Object> sendRequest(@Valid @RequestBody NotificationDTO notificationDTO) {
        log.info("发送交换请求");
        contactRequestService.sendNotification(notificationDTO);
        return RESTful.success(null, "请求已发送");
    }
    @PostMapping("/requests/{requestId}/accept")
    public RESTful<String> acceptRequest(@PathVariable String requestId) {
        log.info("接受交换请求");
        String result =contactRequestService.acceptRequest(requestId);
        return RESTful.success(result, "已同意交换请求");
    }
    @PostMapping("/requests/{requestId}/decline")
    public RESTful<Void> declineRequest(@PathVariable String requestId) {
        log.info("拒绝交换请求");
        contactRequestService.declineRequest(requestId);
        return RESTful.success(null, "已拒绝交换请求");
    }
    /**
     * 获取未处理的通知
     */
    @GetMapping("/unread-count")
    public RESTful<Integer> getUnreadCount() {
        log.info("获取未处理交换请求数量");
        Integer count=contactRequestService.getCountNotification();
        return RESTful.success(count);
    }
}

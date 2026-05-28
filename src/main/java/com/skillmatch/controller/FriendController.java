package com.skillmatch.controller;

import com.skillmatch.domain.vo.FriendVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.IFriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final IFriendService friendService;

    @GetMapping
    public RESTful<List<FriendVO>> getFriends() {
        log.info("获取好友列表");
        List<FriendVO> list = friendService.getFriends();
        return RESTful.success(list);
    }

    @GetMapping("/unread-count")
    public RESTful<Integer> getUnreadCount() {
        log.info("获取未读消息数");
        Integer count = friendService.getUnreadCount();
        return RESTful.success(count);
    }

    @DeleteMapping("/{friendId}")
    public RESTful<Void> removeFriend(@PathVariable String friendId) {
        log.info("删除好友: {}", friendId);
        friendService.removeFriend(friendId);
        return RESTful.success(null, "已删除好友");
    }
}

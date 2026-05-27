package com.skillmatch.controller;

import com.skillmatch.domain.query.MatchingQuery;
import com.skillmatch.domain.vo.PageVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.domain.vo.UserCardVO;
import com.skillmatch.domain.vo.UserProfileVO;
import com.skillmatch.service.IMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {
    private final IMatchingService matchingService;
    /**
     * 获取推荐用户
     */
    @GetMapping("/users")
    public RESTful<PageVO<UserCardVO>> getRecommendedUsers(MatchingQuery query) {
        log.info("获取推荐用户");
        PageVO<UserCardVO> recommendedUsers = matchingService.getRecommendedUsers(query);
        return RESTful.success(recommendedUsers);
    }
    /**
     * 获取匹配用户名片
     */
    @GetMapping("/users/{userId}/card")
    public RESTful<UserCardVO> getUserCard(@PathVariable String userId) {
        log.info("获取用户名片:{}", userId);
        UserCardVO userCard = matchingService.getUserCard(userId);
        return RESTful.success(userCard);
    }

    /**
     * 获取用户完整主页（需登录，查看他人需已交换同意）
     */
    @GetMapping("/users/{userId}/profile")
    public RESTful<UserProfileVO> getUserProfile(@PathVariable String userId) {
        log.info("获取用户完整主页:{}", userId);
        UserProfileVO profile = matchingService.getUserProfile(userId);
        return RESTful.success(profile);
    }
}

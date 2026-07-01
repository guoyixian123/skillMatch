package com.skillmatch.service;

import com.skillmatch.domain.query.MatchingQuery;
import com.skillmatch.domain.vo.PageVO;
import com.skillmatch.domain.vo.UserCardVO;
import com.skillmatch.domain.vo.UserProfileVO;

import java.util.List;

public interface IMatchingService {
    PageVO<UserCardVO> getRecommendedUsers(MatchingQuery query);

    UserCardVO getUserCard(String userId);

    UserProfileVO getUserProfile(String userId);

    /** 获取 LLM 匹配解释（独立接口，不阻塞卡片加载） */
    String getMatchReason(String targetUserId);

    /** 全用户搜索（不走匹配算法，按关键词模糊匹配昵称/技能/爱好/bio） */
    PageVO<UserCardVO> searchUsers(String keyword, int page, int size);

    /** 发现页：随机获取6个匹配用户（支持附近/普通模式，不含匹配分数） */
    List<UserCardVO> getDiscoverUsers(String matchType, List<String> excludeUserIds);

    /** 获取单个用户的匹配分数（延迟加载） */
    int getMatchScore(String targetUserId);

    /** 获取单个用户的 AI 建议（延迟加载，前端异步调用） */
    String getAiSuggestion(String targetUserId);

}

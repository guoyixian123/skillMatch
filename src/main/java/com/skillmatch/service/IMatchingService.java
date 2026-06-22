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

}

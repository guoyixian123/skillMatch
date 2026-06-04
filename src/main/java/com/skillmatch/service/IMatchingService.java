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

}

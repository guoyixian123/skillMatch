package com.skillmatch.service;

import com.skillmatch.domain.query.MatchingQuery;
import com.skillmatch.domain.vo.CityDistributionVO;
import com.skillmatch.domain.vo.PageVO;
import com.skillmatch.domain.vo.ProvinceDistributionVO;
import com.skillmatch.domain.vo.UserCardVO;
import com.skillmatch.domain.vo.UserProfileVO;

import java.util.List;

public interface IMatchingService {
    PageVO<UserCardVO> getRecommendedUsers(MatchingQuery query);

    UserCardVO getUserCard(String userId);

    UserProfileVO getUserProfile(String userId);

    /** 获取城市用户分布（总人数，不区分真人和机器人） */
    List<CityDistributionVO> getCityDistribution();

    /** 获取省份用户分布（用于地图展示） */
    List<ProvinceDistributionVO> getProvinceDistribution();
}

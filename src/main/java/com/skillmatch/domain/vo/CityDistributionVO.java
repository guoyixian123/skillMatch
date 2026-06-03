package com.skillmatch.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 城市用户分布 VO（不区分真人和机器人，只返回总人数）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityDistributionVO {
    /** 城市名 */
    private String city;
    /** 该城市总人数 */
    private Integer count;
}

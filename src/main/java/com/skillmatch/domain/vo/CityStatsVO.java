package com.skillmatch.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityStatsVO {
    // 城市名
    private String city;
    // 真实用户数
    private Long realCount;
    // 机器人数
    private Long robotCount;
    // 总数
    private Long total;
}

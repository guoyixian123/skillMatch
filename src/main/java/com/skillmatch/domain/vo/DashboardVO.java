package com.skillmatch.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 数据统计看板
 */
@Data
public class DashboardVO {
    // 用户总数
    private Long totalUsers;
    // 真人用户数
    private Long realUsers;
    // 机器人用户数
    private Long robotUsers;
    // 正常用户数
    private Long normalUsers;
    // 冻结用户数
    private Long frozenUsers;
    // 真人占比
    private Double realPercent;
    // 机器人占比
    private Double robotPercent;
    // 正常用户占比
    private Double normalPercent;
    // 冻结用户占比
    private Double frozenPercent;

    /** 近7日每日新增 */
    private List<DailyStatsVO> dailyNewUsers;

    /** 地域分布 */
    private List<CityStatsVO> cityDistribution;

    /** 标签分布 */
    private List<TagStatsVO> tagDistribution;

    /** 能教的技能 Top 分布 */
    private List<TagStatsVO> canSkillDistribution;

    /** 想学的技能 Top 分布 */
    private List<TagStatsVO> wantSkillDistribution;

    /** 兴趣爱好 Top 分布 */
    private List<TagStatsVO> hobbyDistribution;
}

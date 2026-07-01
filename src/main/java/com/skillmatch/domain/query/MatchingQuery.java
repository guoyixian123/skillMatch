package com.skillmatch.domain.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.skillmatch.constants.FinalConstant.MATCHING_DISTANCE;

@EqualsAndHashCode(callSuper = true)
@Data
public class MatchingQuery extends PostQuery{
    private String filter;//all=全部 / exchange=技能交换 / partner=找搭子 / active=最近活跃
    private String sort="score";//score=匹配度（默认） / dist=距离 / active=活跃度
    private String keyword;//搜索关键字
    private Integer radius=MATCHING_DISTANCE;//
    /** 匹配类型：nearby=附近匹配(100km) / standard=普通匹配(默认) */
    private String matchType="standard";
    /** 需要排除的用户ID列表（逗号分隔），用于刷新时不重复 */
    private String excludeUserIds;
}

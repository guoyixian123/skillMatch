package com.skillmatch.domain.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MatchingQuery extends PostQuery{
    private String filter;//all=全部 / exchange=技能交换 / partner=找搭子 / active=最近活跃
    private String sort="score";//score=匹配度（默认） / dist=距离 / active=活跃度
    private String keyword;//搜索关键字
    private Integer radius=1000;//
}

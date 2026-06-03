package com.skillmatch.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagStatsVO {
    private String tagName;
    private Long robotCount;
    private Long realCount;
    private Long total;
}

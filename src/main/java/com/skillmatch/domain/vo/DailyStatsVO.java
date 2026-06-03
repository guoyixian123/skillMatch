package com.skillmatch.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatsVO {
    private String date;
    private Long total;
    private Long realCount;
    private Long robotCount;
}

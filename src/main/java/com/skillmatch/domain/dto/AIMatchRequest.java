package com.skillmatch.domain.dto;

import lombok.Data;
import java.util.List;

/** AI 匹配请求 */
@Data
public class AIMatchRequest {
    private AIProfile source;
    private List<AIProfile> candidates;
}

package com.skillmatch.domain.dto;

import lombok.Data;
import java.util.List;

/** AI 匹配响应 */
@Data
public class AIMatchResponse {
    private List<MatchScore> scores;
    private double costMs;

    @Data
    public static class MatchScore {
        private String userId;
        private double semanticSimilarity;
        private double skillComplement;
        private double interestOverlap;
        private double score;
    }
}

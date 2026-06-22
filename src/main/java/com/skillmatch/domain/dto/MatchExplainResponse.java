package com.skillmatch.domain.dto;

import lombok.Data;

/** LLM 匹配解释响应 */
@Data
public class MatchExplainResponse {
    private String reason;
}

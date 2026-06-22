package com.skillmatch.domain.dto;

import lombok.Data;
import java.util.List;

/** LLM 匹配解释请求 */
@Data
public class MatchExplainRequest {
    private String sourceName;
    private String sourceBio = "";
    private List<String> sourceCanSkills;
    private List<String> sourceWantSkills;
    private List<String> sourceHobbies;
    private String targetName;
    private String targetBio = "";
    private List<String> targetCanSkills;
    private List<String> targetWantSkills;
    private List<String> targetHobbies;
    // 非敏感 LLM 配置（由 AIClient 从 application.yaml 注入，API Key 在 AI Engine 侧）
    private String llmBaseUrl = "https://api.deepseek.com/v1";
    private String llmModel = "deepseek-chat";
    private int llmTimeout = 8;
}

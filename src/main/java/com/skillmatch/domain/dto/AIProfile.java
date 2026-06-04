package com.skillmatch.domain.dto;

import lombok.Data;
import java.util.List;

/** AI 用户画像 */
@Data
public class AIProfile {
    private String userId;
    private String bio = "";
    private List<String> canSkills;
    private List<String> wantSkills;
    private List<String> hobbies;
}

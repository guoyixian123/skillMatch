package com.skillmatch.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserCardVO {
    private String userId;
    private String name;
    private String avatarUrl;
    private String signature;
    private String distance;
    private String city;
    private List<String> canSkills;
    private List<String> wantSkills;
    private String bio;
    private int matchScore;
    private Double aiScore;
    private int likeCount;
    private int postCount;
    /** 当前登录用户是否已赞过此用户，用于推荐卡片上的点赞按钮状态 */
    private Boolean isLiked;
    private Boolean hasPendingRequest;
    /** LLM 生成的匹配解释（自然语言），仅卡片详情时有值 */
    private String matchReason;
    /** AI 生成的技能交换建议（自然语言），发现页卡片展示 */
    private String aiSuggestion;
}

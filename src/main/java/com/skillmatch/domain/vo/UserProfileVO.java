package com.skillmatch.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserProfileVO {
    private String userId;
    private String name;
    private String avatarUrl;
    private String coverUrl;
    private String signature;
    private String bio;
    private String contactInfo;
    private String city;
    private List<String> canSkills;
    private List<String> wantSkills;
    private List<HobbyVO> hobbies;
    private List<String> gallery;
    private Integer likeCount;
    private Integer postCount;
    /** 当前登录用户是否已赞过该用户主页，用于前端控制点赞按钮状态 */
    private Boolean isLiked;
    private List<ActivityVO> activities;
}

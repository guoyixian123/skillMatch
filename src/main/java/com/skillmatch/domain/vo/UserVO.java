package com.skillmatch.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private String userId;
    private String name;
    private String password;
    private String avatarUrl;
    private String signature;
    private String bio;
    private String contactInfo;
    private Integer likeCount;
    private Integer postCount;
}

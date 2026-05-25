package com.skillmatch.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {
    private String id;
    private User user;
    private String body;
    private LocalDateTime createdAt;

    //内部类:
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        private String id;
        private String name;
        private String avatarUrl;
    }
}

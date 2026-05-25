package com.skillmatch.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostVO {
    private String id;
    private UserBasicVO author;
    private String body;
    private List<String> tags;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;
}

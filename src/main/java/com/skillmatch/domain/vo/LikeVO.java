package com.skillmatch.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
public class LikeVO {
    /** 点赞后的最新点赞总数 */
    private Integer likeCount;
    /** 当前用户是否已点赞，用于前端切换点赞按钮状态（实心/空心） */
    private Boolean isLiked;
}

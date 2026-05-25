package com.skillmatch.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
public class LikeVO {
    private Integer likeCount;
}

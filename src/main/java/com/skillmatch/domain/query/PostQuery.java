package com.skillmatch.domain.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PostQuery extends PageQuery{
    private String tag;
    private String sort="latest";
    private String keyword;
    private String authorId;
}

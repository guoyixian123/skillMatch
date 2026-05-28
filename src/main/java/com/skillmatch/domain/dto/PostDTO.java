package com.skillmatch.domain.dto;

import com.skillmatch.annotation.ValidTag;
import lombok.Data;

import java.util.List;

@Data
public class PostDTO {
    private String title;
    private String body;
    @ValidTag
    private List<String> tags;
}

package com.skillmatch.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class PostDTO {
    private String title;
    private String body;
    private List<String> tags;
}

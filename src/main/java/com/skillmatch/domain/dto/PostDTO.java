package com.skillmatch.domain.dto;

import com.skillmatch.annotation.ValidTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PostDTO {
    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 100, message = "标题1-100字")
    private String title;
    @NotBlank(message = "内容不能为空")
    @Size(max = 2000, message = "内容最长2000字")
    private String body;
    @ValidTag
    @Size(max = 10, message = "标签最多10个")
    private List<String> tags;
}

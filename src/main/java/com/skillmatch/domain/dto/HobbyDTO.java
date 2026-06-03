package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HobbyDTO {
    @NotBlank(message = "爱好名不能为空")
    @Size(max = 20, message = "爱好名最长20字")
    private String hobbyName;
    @Size(max = 10, message = "图标最长10字")
    private String icon;
}

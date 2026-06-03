package com.skillmatch.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserSkillDTO {
    @NotBlank(message = "技能名不能为空")
    @Size(max = 30, message = "技能名最长30字")
    private String skillName;
    @NotNull(message = "技能类型不能为空")
    @Min(value = 1, message = "类型只能为1或2")
    @Max(value = 2, message = "类型只能为1或2")
    private int skillType;
}

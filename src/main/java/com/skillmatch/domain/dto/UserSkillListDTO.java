package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
@Data
public class UserSkillListDTO {
    @NotNull(message = "技能列表不能为空")
    @Size(max = 10, message = "技能最多10个")
    private List<@NotBlank(message = "技能名不能为空")String> canSkills;
    @NotNull(message = "技能列表不能为空")
    @Size(max = 10, message = "技能最多10个")
    private List<@NotBlank(message = "技能名不能为空")String> wantSkills;
}

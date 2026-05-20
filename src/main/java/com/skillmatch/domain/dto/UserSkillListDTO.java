package com.skillmatch.domain.dto;

import lombok.Data;

import java.util.List;
@Data
public class UserSkillListDTO {
    private List<String> canSkills;
    private List<String> wantSkills;
}

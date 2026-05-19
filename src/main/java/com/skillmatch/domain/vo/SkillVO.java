package com.skillmatch.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkillVO {
    private List<Skill> canSkills;
    private List<Skill> wantSkills;
}

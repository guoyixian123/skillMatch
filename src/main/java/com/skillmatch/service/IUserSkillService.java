package com.skillmatch.service;

import com.skillmatch.domain.dto.UserSkillDTO;
import com.skillmatch.domain.dto.UserSkillListDTO;
import com.skillmatch.domain.po.UserSkill;
import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.vo.SkillVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
public interface IUserSkillService extends IService<UserSkill> {

    SkillVO getUserAndSkillInfo();

    void addUserSkills(UserSkillDTO skill);

    void updateUserSkillList(UserSkillListDTO  skills);

    void removeUserSkillById(String skillId);
}

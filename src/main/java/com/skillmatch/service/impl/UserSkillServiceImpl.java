package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.skillmatch.context.BeanContext;
import com.skillmatch.domain.po.UserSkill;
import com.skillmatch.domain.vo.Skill;
import com.skillmatch.domain.vo.SkillVO;
import com.skillmatch.mapper.UserSkillMapper;
import com.skillmatch.service.IUserSkillService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
@Service
public class UserSkillServiceImpl extends ServiceImpl<UserSkillMapper, UserSkill> implements IUserSkillService {

    /**
     * 获取用户技能信息
     */
    @Override
    public SkillVO getUserAndSkillInfo() {
        String userId = BeanContext.getUserId();
        if(userId == null||userId.isEmpty()) return null;
        List<UserSkill> list = lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .list();
        if(list.isEmpty()){
            return new SkillVO();
        }
        Map<Integer, List<UserSkill>> skillTypeMap = list.stream().collect(Collectors.groupingBy(UserSkill::getSkillType));
        //用户的所有会的技能
        List<UserSkill> userSkills = skillTypeMap.get(1);
        //封装到vo中
        List<Skill> canSkills = new ArrayList<>(userSkills.size());
        for (UserSkill userSkill : userSkills){
            Skill skill = BeanUtil.copyProperties(userSkill, Skill.class);
            canSkills.add(skill);
        }
        //用户的所有想学的技能
        List<UserSkill> wantSkills = skillTypeMap.get(2);
        List<Skill> wantSkill = new ArrayList<>(wantSkills.size());
        for (UserSkill userSkill : wantSkills){
            Skill skill = BeanUtil.copyProperties(userSkill, Skill.class);
            wantSkill.add(skill);
        }
        return new SkillVO(canSkills,wantSkill);
    }
}

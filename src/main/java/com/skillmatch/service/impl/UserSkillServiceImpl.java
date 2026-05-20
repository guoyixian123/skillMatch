package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.skillmatch.context.BeanContext;
import com.skillmatch.domain.dto.UserSkillDTO;
import com.skillmatch.domain.dto.UserSkillListDTO;
import com.skillmatch.domain.po.UserSkill;
import com.skillmatch.domain.vo.Skill;
import com.skillmatch.domain.vo.SkillVO;
import com.skillmatch.mapper.UserSkillMapper;
import com.skillmatch.service.IUserSkillService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
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
        if (userId == null || userId.isEmpty()) return null;
        List<UserSkill> list = lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .list();
        if (list.isEmpty()) {
            return new SkillVO();
        }
        Map<Integer, List<UserSkill>> skillTypeMap = list.stream().collect(Collectors.groupingBy(UserSkill::getSkillType));
        //用户的所有会的技能
        List<UserSkill> userSkills = skillTypeMap.get(1);
        //封装到vo中
        List<Skill> canSkills = new ArrayList<>(userSkills.size());
        for (UserSkill userSkill : userSkills) {
            Skill skill = BeanUtil.copyProperties(userSkill, Skill.class);
            canSkills.add(skill);
        }
        //用户的所有想学的技能
        List<UserSkill> wantSkills = skillTypeMap.get(2);
        List<Skill> wantSkill = new ArrayList<>(wantSkills.size());
        for (UserSkill userSkill : wantSkills) {
            Skill skill = BeanUtil.copyProperties(userSkill, Skill.class);
            wantSkill.add(skill);
        }
        return new SkillVO(canSkills, wantSkill);
    }

    /**
     * 添加用户技能
     */
    @Override
    public void addUserSkills(UserSkillDTO skill) {
        String userId = BeanContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("用户未登录");
        }
        //技能类型转换1: 会 can 2: 想学 want
        Integer type = null;
        if (Objects.equals(skill.getSkillType(), "can")) {
            type = 1;
        }
        if (Objects.equals(skill.getSkillType(), "want")) {
            type = 2;
        }
        //查询用户已有的技能数量,并且判断是否超过20个
        Long count1 = lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillType, type)
                .count();
        if (count1 > 20) {
            throw new RuntimeException("技能数量已经超过20个");
        }
        Long count2 = lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillType, type)
                .count();
        if (count2 > 20) {
            throw new RuntimeException("技能数量已经超过20个");
        }
        //添加技能
        UserSkill userSkill = BeanUtil.copyProperties(skill, UserSkill.class, "skillType");
        userSkill.setSkillType(type);
        userSkill.setUserId(userId);
        userSkill.setCreatedTime(LocalDateTime.now());
        save(userSkill);
    }

    /**
     * 批量修改用户技能
     */
    @Transactional//多次修改数据库需要修改添加事务
    @Override
    public void updateUserSkillList(UserSkillListDTO skills) {
        String userId = BeanContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("用户未登录");
        }
        //判断技能数量是否超过20个
        if (skills.getCanSkills().size() > 20 || skills.getWantSkills().size() > 20) {
            throw new RuntimeException("技能数量已经超过20个");
        }
        //先删该用户技能
        boolean remove = lambdaUpdate()
                .eq(UserSkill::getUserId, userId)
                .remove();
        if (!remove) {
            throw new RuntimeException("删除技能失败");
        }
        // 再添加
        //先封装我会的技能
        List<UserSkill> canSkills = new ArrayList<>(skills.getCanSkills().size());
        for (String skillName : skills.getCanSkills()) {
            UserSkill userSkill = new UserSkill();
            userSkill.setSkillType(1);
            userSkill.setSkillName(skillName);
            userSkill.setUserId(userId);
            userSkill.setCreatedTime(LocalDateTime.now());
            canSkills.add(userSkill);
        }
        //再封装想学的技能
        List<UserSkill> wantSkills = new ArrayList<>(skills.getWantSkills().size());
        for (String skillName : skills.getWantSkills()) {
            UserSkill userSkill = new UserSkill();
            userSkill.setSkillType(2);
            userSkill.setSkillName(skillName);
            userSkill.setUserId(userId);
            userSkill.setCreatedTime(LocalDateTime.now());
            wantSkills.add(userSkill);
        }
        //批量保存
        saveBatch(canSkills);
        saveBatch(wantSkills);
    }
}

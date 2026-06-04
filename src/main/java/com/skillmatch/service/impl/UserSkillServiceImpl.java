package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.UserSkillDTO;
import com.skillmatch.domain.dto.UserSkillListDTO;
import com.skillmatch.domain.po.UserSkill;
import com.skillmatch.domain.vo.SkillItemVO;
import com.skillmatch.domain.vo.SkillVO;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.UserSkillMapper;
import com.skillmatch.service.ITagService;
import com.skillmatch.service.IUserSkillService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private ITagService tagService;

    /**
     * 获取用户技能信息
     */
    @Override
    public SkillVO getUserAndSkillInfo() {
        String userId = UserContext.getUserId();
        //查询用户所有技能
        List<UserSkill> list = lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .list();
        if (list == null||list.isEmpty()) {
            //返回空
            return new SkillVO();
        }
        //根据技能类型分组1:can 2:want
        Map<Integer, List<UserSkill>> skillTypeMap = list.stream().collect(Collectors.groupingBy(UserSkill::getSkillType));
        //用户的所有会的技能
        List<UserSkill> userSkills = skillTypeMap.getOrDefault(1, List.of());
        //封装到vo中
        List<SkillItemVO> canSkills = new ArrayList<>(userSkills.size());
        for (UserSkill userSkill : userSkills) {
            SkillItemVO skill = BeanUtil.copyProperties(userSkill, SkillItemVO.class);
            canSkills.add(skill);
        }
        //用户的所有想学的技能
        List<UserSkill> wantSkills = skillTypeMap.getOrDefault(2, List.of());
        List<SkillItemVO> wantSkill = new ArrayList<>(wantSkills.size());
        for (UserSkill userSkill : wantSkills) {
            SkillItemVO skill = BeanUtil.copyProperties(userSkill, SkillItemVO.class);
            wantSkill.add(skill);
        }
        return new SkillVO(canSkills, wantSkill);
    }

    /**
     * 添加用户技能
     */
    @Override
    public void addUserSkills(UserSkillDTO skill) {
        String userId = UserContext.getUserId();
        if(skill==null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        if (!tagService.isValidSkillName(skill.getSkillName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的技能标签");
        }

        //查询用户已有的技能数量,并且判断是否超过10个
        Long count = lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillType, skill.getSkillType())
                .count();
        if (count > 10) {
            throw new BusinessException(ErrorCode.EXCEED_LIMIT, "技能数量已经超过10个");
        }
        //添加技能到数据库
        UserSkill userSkill = BeanUtil.copyProperties(skill, UserSkill.class, "skillType");
        userSkill.setSkillType(skill.getSkillType());
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
        String userId = UserContext.getUserId();
        //判断技能数量是否超过20个
        if (skills.getCanSkills().size() > 10 || skills.getWantSkills().size() > 10) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,"技能数量已经超过10个");
        }
        //先删该用户技能
        boolean remove = lambdaUpdate()
                .eq(UserSkill::getUserId, userId)
                .remove();
        // 再添加
        //先封装我会的技能
        List<UserSkill> list = new ArrayList<>(skills.getCanSkills().size());

        for (String skillName : skills.getCanSkills()) {
            UserSkill userSkill = new UserSkill();
            userSkill.setSkillType(1);
            userSkill.setSkillName(skillName);
            userSkill.setUserId(userId);
            userSkill.setCreatedTime(LocalDateTime.now());
            list.add(userSkill);
        }
        //再封装想学的技能
        for (String skillName : skills.getWantSkills()) {
            UserSkill userSkill = new UserSkill();
            userSkill.setSkillType(2);
            userSkill.setSkillName(skillName);
            userSkill.setUserId(userId);
            userSkill.setCreatedTime(LocalDateTime.now());
            list.add(userSkill);
        }
        //批量保存
        saveBatch(list);
    }

    @Override
    public void removeUserSkillById(String skillId) {
        //获取用户id
        String userId = UserContext.getUserId();
        //获取技能
        if(skillId==null) throw new BusinessException(ErrorCode.PARAM_ERROR);
        UserSkill skill = getById(skillId);
        if(skill==null) throw new BusinessException(ErrorCode.NOT_FOUND, "技能不存在");
        if(skillId==null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        //判断是否是自己的技能,所有权校验
        if (!skill.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_AUTH, "无权操作");
        }
        boolean b = removeById(skillId);
        if (!b) {
            throw new BusinessException(ErrorCode.SERVER_ERROR,"删除技能失败");
        }
    }
}

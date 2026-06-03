package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.HobbyDTO;
import com.skillmatch.domain.po.UserHobby;
import com.skillmatch.domain.vo.HobbyVO;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.UserHobbyMapper;
import com.skillmatch.service.ITagService;
import com.skillmatch.service.IUserHobbyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Speed
 * @since 2026-05-20
 */
@Service
public class UserHobbyServiceImpl extends ServiceImpl<UserHobbyMapper, UserHobby> implements IUserHobbyService {

    @Autowired
    private ITagService tagService;
    /**
     * 获取用户爱好信息
     */
    @Override
    public List<HobbyVO> getUserHobbyInfo() {
        String userId = UserContext.getUserId();
        //查出用户爱好信息
        List<UserHobby> list = lambdaQuery()
                .eq(UserHobby::getUserId, userId)
                .list();
        //封装vo
        List<HobbyVO> hobbyList = new ArrayList<>(list.size());
        for (UserHobby userHobby : list) {
            HobbyVO hobbyVO = BeanUtil.copyProperties(userHobby, HobbyVO.class);
            hobbyList.add(hobbyVO);
        }
        //转为list集合
        return  hobbyList;
    }

    @Override
    public void addUserHobbies(HobbyDTO hobby) {
        if(hobby == null) throw new BusinessException(ErrorCode.PARAM_ERROR);
        if (!tagService.isValidHobbyName(hobby.getHobbyName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的爱好标签");
        }
        String userId = UserContext.getUserId();
        //封装
        UserHobby userHobby = BeanUtil.copyProperties(hobby, UserHobby.class);
        userHobby.setUserId(userId);
        userHobby.setIcon(tagService.getHobbyIcon(hobby.getHobbyName()));
        userHobby.setCreateAt(LocalDateTime.now());
        save(userHobby);
    }

    @Override
    public void removeHobbyById(String hobbyId) {
        //获取用户id
        String userId = UserContext.getUserId();
        //获取爱好
        UserHobby hobby = getById(hobbyId);
        if(hobbyId == null) throw new BusinessException(ErrorCode.PARAM_ERROR);
        //判断爱好是否属于当前用户
        if (!hobby.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_AUTH, "不能删除他人的爱好");
        }
        boolean b = removeById(hobbyId);
        if (!b) {
            throw new BusinessException(ErrorCode.SERVER_ERROR,"删除爱好失败");
        }
    }
}

package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.skillmatch.context.BeanContext;
import com.skillmatch.domain.dto.HobbyDTO;
import com.skillmatch.domain.po.UserHobby;
import com.skillmatch.domain.vo.HobbyListVO;
import com.skillmatch.domain.vo.HobbyVO;
import com.skillmatch.mapper.UserHobbyMapper;
import com.skillmatch.service.IUserHobbyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

    @Override
    public HobbyListVO getUserHobbyInfo() {
        String userId = BeanContext.getUserId();
        //查出用户爱好信息
        List<UserHobby> list = lambdaQuery()
                .eq(UserHobby::getUserId, userId)
                .list();
        //封装vo
        List<HobbyVO> hobbyVOS = new ArrayList<>(list.size());
        for (UserHobby userHobby : list) {
            HobbyVO hobbyVO = BeanUtil.copyProperties(userHobby, HobbyVO.class);
            hobbyVOS.add(hobbyVO);
        }
        //转为list集合
        HobbyListVO hobbyList = new HobbyListVO();
        hobbyList.setList(hobbyVOS);
        return  hobbyList;
    }

    @Override
    public void addUserHobbies(HobbyDTO hobby) {
        if(hobby == null) throw new RuntimeException("参数错误");
        String userId = BeanContext.getUserId();
        if(userId == null || userId.isEmpty()) throw new RuntimeException("用户未登录");
        UserHobby userHobby = BeanUtil.copyProperties(hobby, UserHobby.class);
        userHobby.setUserId(userId);
        userHobby.setCreateAt(LocalDateTime.now());
        save(userHobby);
    }

    @Override
    public void removeHobbyById(String hobbyId) {
        removeById(hobbyId);
    }
}

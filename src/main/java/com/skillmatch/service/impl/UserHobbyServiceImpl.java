package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.HobbyDTO;
import com.skillmatch.domain.po.UserHobby;
import com.skillmatch.domain.vo.HobbyVO;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
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
        String userId = UserContext.getUserId();
        //封装
        UserHobby userHobby = BeanUtil.copyProperties(hobby, UserHobby.class);
        userHobby.setUserId(userId);
        userHobby.setCreateAt(LocalDateTime.now());
        save(userHobby);
    }

    @Override
    public void removeHobbyById(String hobbyId) {
        if(hobbyId == null) throw new BusinessException(ErrorCode.PARAM_ERROR);
        boolean b = removeById(hobbyId);
        if (!b) {
            throw new BusinessException(ErrorCode.SERVER_ERROR,"删除爱好失败");
        }
    }
}

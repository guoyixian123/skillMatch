package com.skillmatch.service;

import com.skillmatch.domain.dto.HobbyDTO;
import com.skillmatch.domain.po.UserHobby;
import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.vo.HobbyVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Speed
 * @since 2026-05-20
 */
public interface IUserHobbyService extends IService<UserHobby> {

    List<HobbyVO> getUserHobbyInfo();

    void addUserHobbies(HobbyDTO hobby);

    void removeHobbyById(String hobbyId);
}

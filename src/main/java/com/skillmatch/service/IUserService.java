package com.skillmatch.service;

import com.skillmatch.domain.dto.LocationDTO;
import com.skillmatch.domain.dto.PasswordDTO;
import com.skillmatch.domain.dto.UserDTO;
import com.skillmatch.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
public interface IUserService extends IService<User> {
    UserVO getProfile(String  userId);

    void updateUser(UserDTO userInfo);

    void uploadAvatar(MultipartFile avatarUrl);

    void updatePassword(PasswordDTO password);

    void updateLocation(LocationDTO location);
}

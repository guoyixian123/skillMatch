package com.skillmatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.dto.RegisterAndLogin;
import com.skillmatch.domain.po.User;
import java.util.Map;

public interface IAuthService extends IService<User> {
    Map<String,Object> register(RegisterAndLogin register);

    Map<String, Object> login(RegisterAndLogin login);

    String refreshToken();

    void logout();
}

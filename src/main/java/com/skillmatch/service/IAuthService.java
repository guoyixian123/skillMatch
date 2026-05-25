package com.skillmatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.dto.RegisterAndLoginDTO;
import com.skillmatch.domain.po.User;
import java.util.Map;

public interface IAuthService extends IService<User> {
    Map<String,Object> register(RegisterAndLoginDTO register);

    Map<String, Object> login(RegisterAndLoginDTO login);

    String refreshToken();

    void logout();
}

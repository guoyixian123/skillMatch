package com.skillmatch.controller;

import com.skillmatch.context.BeanContext;
import com.skillmatch.domain.dto.RegisterAndLogin;
import com.skillmatch.domain.dto.TokenDto;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService IAuthService;
    @PostMapping("/register")
    public RESTful<Object> register(@RequestBody RegisterAndLogin register) {
        log.info("用户注册: {}", register);
        Map<String, Object> map = IAuthService.register(register);
        return RESTful.success(map);
    }
    @PostMapping("/login")
    public RESTful<Object> login(@RequestBody RegisterAndLogin login) {
        log.info("用户登录: {}", login);
        Map<String, Object> map = IAuthService.login(login);
        return RESTful.success(map);
    }
    @PostMapping("/refresh")
    public RESTful<String> refreshToken() {
        String newToken = IAuthService.refreshToken();
        return RESTful.success(newToken);
    }
    @PostMapping("/logout")
    public RESTful<Object> logout() {
        log.info("用户退出登录: {}", BeanContext.getUserId());
        IAuthService.logout();
        return RESTful.success();
    }
}

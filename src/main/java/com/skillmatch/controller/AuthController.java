package com.skillmatch.controller;

import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.RegisterAndLoginDTO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService IAuthService;
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public RESTful<Object> register(@Valid @RequestBody RegisterAndLoginDTO register) {
        log.info("用户注册: {}", register);
        Map<String, Object> map = IAuthService.register(register);
        return RESTful.success(map);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public RESTful<Object> login(@Valid @RequestBody RegisterAndLoginDTO login) {
        log.info("用户登录: {}", login);
        Map<String, Object> map = IAuthService.login(login);
        return RESTful.success(map);
    }

    /**
     * 刷新token
     */
    @PostMapping("/refresh")
    public RESTful<String> refreshToken() {
        String newToken = IAuthService.refreshToken();
        return RESTful.success(newToken);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public RESTful<Object> logout() {
        log.info("用户退出登录: {}", UserContext.getUserId());
        IAuthService.logout();
        return RESTful.success();
    }
}

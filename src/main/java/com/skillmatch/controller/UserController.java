package com.skillmatch.controller;


import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.LocationDTO;
import com.skillmatch.domain.dto.PasswordDTO;
import com.skillmatch.domain.dto.UserDTO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.domain.vo.UserVO;
import com.skillmatch.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;

    /**
     * 获取用户信息
     */
    @GetMapping("/profile/{userId}")
    public RESTful<UserVO> profile(@PathVariable String userId) {
        log.info("获取用户信息:{}", userId);
        UserVO profile = userService.getProfile(userId);
        return RESTful.success(profile);
    }

    /**
     * 修改用户信息
     */
    @PutMapping("/profile")
    public RESTful<Object> profile(@Valid @RequestBody UserDTO userInfo) {
        log.info("修改用户信息:{}", userInfo);
        userService.updateUser(userInfo);
        return RESTful.success(null, "资料已经更新");
    }

    /**
     * 更新用户头像
     */
    @PutMapping("/avatar")
    public RESTful<String> uploadAvatar(@RequestParam MultipartFile avatarUrl) {
        log.info("更新用户id:{}的头像", UserContext.getUserId());
        String url = userService.uploadAvatar(avatarUrl);
        return RESTful.success(url, "头像已经更新");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public RESTful<Object> updatePassword(@Valid @RequestBody PasswordDTO password) {
        log.info("修改用户id:{}的密码", UserContext.getUserId());
        userService.updatePassword(password);
        return RESTful.success(null, "密码已经更新");
    }

    /**
     * 一键删除BOT
     */
    @DeleteMapping("/account")
    public RESTful<Object> account() {
        userService.removeBotInfo();
        return RESTful.success(null);
    }

    /**
     * 获取用户地理位置
     */
    @PutMapping("/location")
    public RESTful<Object> updateLocation(@Valid @RequestBody LocationDTO location) {
        log.info("修改用户id:{}的地理位置:经度->{} 纬度->{}", UserContext.getUserId(), location.getLongitude(), location.getLatitude());
        userService.updateLocation(location);
        return RESTful.success(null, "位置已经更新");
    }

}

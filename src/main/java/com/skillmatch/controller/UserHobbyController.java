package com.skillmatch.controller;


import com.skillmatch.domain.dto.HobbyDTO;
import com.skillmatch.domain.vo.HobbyVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.IUserHobbyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Speed
 * @since 2026-05-20
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserHobbyController {
    private final IUserHobbyService userHobbyService;
    /**
     * 获取用户爱好信息
     */
    @GetMapping("/hobbies")
    public RESTful<List<HobbyVO>> getUserHobbies() {
        log.info("获取用户爱好信息");
        List<HobbyVO> hobbyInfo = userHobbyService.getUserHobbyInfo();
       return RESTful.success(hobbyInfo);
    }
    /**
     * 添加用户爱好
     */
    @PostMapping("/hobbies")
    public RESTful<Void> addUserHobbies(@RequestBody HobbyDTO hobby) {
        log.info("添加用户爱好:{}", hobby);
        userHobbyService.addUserHobbies(hobby);
        return RESTful.success();
    }
    /**
     * 删除用户爱好
     */
    @DeleteMapping("/hobbies/{hobbyId}")
    public RESTful<Void> deleteUserHobbies(@PathVariable String hobbyId) {
        log.info("删除用户爱好:爱好id=>{}", hobbyId);
        userHobbyService.removeHobbyById(hobbyId);
        return RESTful.success();
    }
}

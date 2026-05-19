package com.skillmatch.controller;


import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.domain.vo.SkillVO;
import com.skillmatch.service.IUserSkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserSkillController {
    private final IUserSkillService userSkillService;
    /**
     * 获取用户技能信息
     */
    @GetMapping("/skills")
    public RESTful<Object> getUserSkills() {
        log.info("获取用户技能信息");
        SkillVO info = userSkillService.getUserAndSkillInfo();
        return RESTful.success(info);
    }

}

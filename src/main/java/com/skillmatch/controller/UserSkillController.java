package com.skillmatch.controller;


import com.skillmatch.domain.dto.UserSkillDTO;
import com.skillmatch.domain.dto.UserSkillListDTO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.domain.vo.Skill;
import com.skillmatch.domain.vo.SkillVO;
import com.skillmatch.service.IUserSkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    /**
     * 添加用户技能
     */
    @PostMapping("/skills")
    public RESTful<Object> addUserSkills(@RequestBody UserSkillDTO skill) {
        log.info("添加用户技能信息:{}", skill);
        userSkillService.addUserSkills(skill);
        return RESTful.success(null);
    }
    /**
     * 删除用户技能
     */
    @DeleteMapping("/skills/{skillId}")
    public RESTful<Object> deleteUserSkills(@PathVariable String skillId) {
        log.info("删除用户技能信息:技能id=>{}", skillId);
        userSkillService.removeById(skillId);
        return RESTful.success(null);
    }
    /**
     * 批量修改用户技能
     */
    @PutMapping("/skills")
    public RESTful<Object> updateUserSkills(@RequestBody UserSkillListDTO  skills) {
        log.info("批量修改用户技能信息:{}", skills);
        userSkillService.updateUserSkillList(skills);
        return RESTful.success(null);
    }

}

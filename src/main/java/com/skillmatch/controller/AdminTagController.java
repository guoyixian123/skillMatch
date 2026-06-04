package com.skillmatch.controller;

import com.skillmatch.domain.po.HobbyTag;
import com.skillmatch.domain.po.SkillTag;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.mapper.HobbyTagMapper;
import com.skillmatch.mapper.SkillTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 技能标签 & 爱好标签 CRUD
 */
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

    private final SkillTagMapper skillTagMapper;
    private final HobbyTagMapper hobbyTagMapper;

    // ==================== 技能标签 ====================

    /** 获取所有技能标签（平铺列表，含 id） */
    @GetMapping("/skills")
    public RESTful<List<SkillTag>> listSkillTags() {
        return RESTful.success(skillTagMapper.selectList(null));
    }

    /** 新增技能标签 */
    @PostMapping("/skills")
    public RESTful<Void> createSkillTag(@RequestBody SkillTag tag) {
        skillTagMapper.insert(tag);
        return RESTful.success(null, "新增成功");
    }

    /** 编辑技能标签 */
    @PutMapping("/skills/{id}")
    public RESTful<Void> updateSkillTag(@PathVariable Long id, @RequestBody SkillTag tag) {
        tag.setId(id);
        skillTagMapper.updateById(tag);
        return RESTful.success(null, "修改成功");
    }

    /** 删除技能标签 */
    @DeleteMapping("/skills/{id}")
    public RESTful<Void> deleteSkillTag(@PathVariable Long id) {
        skillTagMapper.deleteById(id);
        return RESTful.success(null, "删除成功");
    }

    // ==================== 爱好标签 ====================

    /** 获取所有爱好标签（平铺列表，含 id） */
    @GetMapping("/hobbies")
    public RESTful<List<HobbyTag>> listHobbyTags() {
        return RESTful.success(hobbyTagMapper.selectList(null));
    }

    /** 新增爱好标签 */
    @PostMapping("/hobbies")
    public RESTful<Void> createHobbyTag(@RequestBody HobbyTag tag) {
        hobbyTagMapper.insert(tag);
        return RESTful.success(null, "新增成功");
    }

    /** 编辑爱好标签 */
    @PutMapping("/hobbies/{id}")
    public RESTful<Void> updateHobbyTag(@PathVariable Long id, @RequestBody HobbyTag tag) {
        tag.setId(id);
        hobbyTagMapper.updateById(tag);
        return RESTful.success(null, "修改成功");
    }

    /** 删除爱好标签 */
    @DeleteMapping("/hobbies/{id}")
    public RESTful<Void> deleteHobbyTag(@PathVariable Long id) {
        hobbyTagMapper.deleteById(id);
        return RESTful.success(null, "删除成功");
    }
}

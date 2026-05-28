package com.skillmatch.controller;

import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.ITagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final ITagService tagService;

    @GetMapping("/skills")
    public RESTful<Map<String, List<String>>> getSkillTags() {
        return RESTful.success(tagService.getSkillTagsByCategory());
    }

    @GetMapping("/hobbies")
    public RESTful<Map<String, List<Map<String, String>>>> getHobbyTags() {
        return RESTful.success(tagService.getHobbyTagsByCategory());
    }
}

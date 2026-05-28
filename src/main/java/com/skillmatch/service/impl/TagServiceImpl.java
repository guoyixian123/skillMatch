package com.skillmatch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.domain.po.HobbyTag;
import com.skillmatch.domain.po.SkillTag;
import com.skillmatch.mapper.HobbyTagMapper;
import com.skillmatch.mapper.SkillTagMapper;
import com.skillmatch.service.ITagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<SkillTagMapper, SkillTag> implements ITagService {

    private final SkillTagMapper skillTagMapper;
    private final HobbyTagMapper hobbyTagMapper;

    @Override
    public Map<String, List<String>> getSkillTagsByCategory() {
        List<SkillTag> tags = skillTagMapper.selectList(null);
        return tags.stream()
                .sorted(Comparator.comparing(SkillTag::getSortOrder))
                .collect(Collectors.groupingBy(
                        SkillTag::getCategory,
                        LinkedHashMap::new,
                        Collectors.mapping(SkillTag::getName, Collectors.toList())
                ));
    }

    @Override
    public Map<String, List<Map<String, String>>> getHobbyTagsByCategory() {
        List<HobbyTag> tags = hobbyTagMapper.selectList(null);
        return tags.stream()
                .sorted(Comparator.comparing(HobbyTag::getSortOrder))
                .collect(Collectors.groupingBy(
                        HobbyTag::getCategory,
                        LinkedHashMap::new,
                        Collectors.mapping(t -> {
                            Map<String, String> m = new LinkedHashMap<>();
                            m.put("name", t.getName());
                            m.put("icon", t.getIcon());
                            return m;
                        }, Collectors.toList())
                ));
    }

    @Override
    public boolean isValidSkillName(String name) {
        if (name == null || name.isBlank()) return false;
        return skillTagMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SkillTag>()
                        .eq("name", name.trim())
        ) > 0;
    }

    @Override
    public boolean isValidHobbyName(String name) {
        if (name == null || name.isBlank()) return false;
        return hobbyTagMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<HobbyTag>()
                        .eq("name", name.trim())
        ) > 0;
    }

    @Override
    public String getHobbyIcon(String name) {
        if (name == null || name.isBlank()) return "✨";
        HobbyTag tag = hobbyTagMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<HobbyTag>()
                        .eq("name", name.trim())
                        .last("LIMIT 1")
        );
        return tag != null ? tag.getIcon() : "✨";
    }
}

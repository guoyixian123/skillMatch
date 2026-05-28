package com.skillmatch.service;

import java.util.List;
import java.util.Map;

public interface ITagService {
    Map<String, List<String>> getSkillTagsByCategory();
    Map<String, List<Map<String, String>>> getHobbyTagsByCategory();
    boolean isValidSkillName(String name);
    boolean isValidHobbyName(String name);
    String getHobbyIcon(String name);
}

package com.skillmatch.service.impl;

import com.skillmatch.context.UserContext;
import com.skillmatch.domain.po.UserSkill;
import com.skillmatch.utils.GeoUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class UserSkillServiceImplTest {
    @Autowired
    private  GeoUtil geoUtil;
    @Test
    void getUserAndSkillInfo() {
        String s = geoUtil.resolveCity(116.397128, 39.90816);
        System.out.println(s);
    }
}
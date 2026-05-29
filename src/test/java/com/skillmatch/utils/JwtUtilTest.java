package com.skillmatch.utils;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class JwtUtilTest {

    @Test
    void createToken() {
        String token = JwtUtil.createToken("1", "xiaoXin666");
        System.out.println(token);
        Map<String, String> map = JwtUtil.parseToken(token);
        System.out.println(map);
    }
}
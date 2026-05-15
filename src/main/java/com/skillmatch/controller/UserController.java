package com.skillmatch.controller;


import com.skillmatch.domain.vo.RESTful;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @GetMapping("/test")
    public RESTful<List<String>> test() {
        List<String> list = new ArrayList<>();
        list.add("惠");
        list.add("惠");
        list.add("是");
        list.add("小");
        list.add("🐷");
        log.info("{}", list);
        return RESTful.success(list);
    }

}

package com.skillmatch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.skillmatch.mapper")
public class SkillMatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillMatchApplication.class, args);
        String[] art = {"""
_                      _         __    __    __\s
__  __(_) __ _  ___  __  __ _(_)_ __   / /_  / /_  / /_
\\ \\/ /| |/ _` |/ _ \\ \\ \\/ /| | | '_ \\ | '_ \\| '_ \\| '_ \\
 >  < | | (_| | (_) | >  < | | | | | || (_) | (_) | (_) |
/_/\\_\\|_|\\__,_|\\___/ /_/\\_\\|_|_|_| |_| \\___/ \\___/ \\___/

 >> Developer: Guo Jiaxin (郭嘉欣)
 >> project name:skillMatch
 >> create firstTime 2026_05_15
 """};
        for (String line : art) {
            System.out.println(line);
        }
    }

}

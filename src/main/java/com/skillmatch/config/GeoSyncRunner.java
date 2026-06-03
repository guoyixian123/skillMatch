package com.skillmatch.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skillmatch.domain.po.User;
import com.skillmatch.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.skillmatch.constants.RedisConstant.USER_LOCATION_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoSyncRunner implements CommandLineRunner {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) {
        syncAllUsersToGeo();  // 启动时仍同步一次
    }

    /** 公开方法：从 MySQL 全量同步用户位置到 Redis GEO */
    public void syncAllUsersToGeo() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        // 排除无位置数据ne:!=0
                        .ne(User::getLatitude, 0)
                        .ne(User::getLongitude, 0)
        );
        if (users.isEmpty()) {
            log.info("GeoSync: 无用户位置数据，跳过");
            return;
        }
        for (User u : users) {
            redisTemplate.opsForGeo().add(
                    USER_LOCATION_KEY,
                    new Point(u.getLongitude(), u.getLatitude()),
                    u.getUserId()
            );
        }
        log.info("GeoSync: 已同步 {} 个用户位置到 Redis GEO", users.size());
    }

}

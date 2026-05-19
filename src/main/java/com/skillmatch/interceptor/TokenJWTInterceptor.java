package com.skillmatch.interceptor;

import com.skillmatch.context.BeanContext;
import com.skillmatch.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TokenJWTInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token
        String token = request.getHeader("user_info");
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        try {
            // 解析token
            String userId = JwtUtil.parseToken(token).get("userId");
            //判断在redis中是否存在
            if (!(redisTemplate.hasKey("login:token:"+userId)&&Objects.equals(redisTemplate.opsForValue().get("login:token:" + userId), token))) {
                throw new Exception("用户不存在");
            }
            //放入线程中
            BeanContext.setUserId(userId);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }

    }
}

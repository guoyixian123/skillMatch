package com.skillmatch.interceptor;

import com.skillmatch.context.UserContext;
import com.skillmatch.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

import static com.skillmatch.constants.RedisConstant.LOGIN_TOKEN_KEY;

@Component
@RequiredArgsConstructor
public class TokenJWTInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // 获取请求头中的token
        String token = request.getHeader("user_info");
        // log.debug("token:{}", token);
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        try {
            // 解析token
            String userId = JwtUtil.parseToken(token).get("userId");
            // 判断token是否为空
            if (userId == null || userId.isEmpty()) {
                response.setStatus(401);
                return false;
            }
            //判断在redis中是否存在（单次get替代hasKey+get，减少一次Redis RTT）
            String cachedToken = redisTemplate.opsForValue().get(LOGIN_TOKEN_KEY + userId);
            if (cachedToken == null || !cachedToken.equals(token)) {
                response.setStatus(401);
                return false;
            }
            //放入线程中
            UserContext.setUserId(userId);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }

    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }
}

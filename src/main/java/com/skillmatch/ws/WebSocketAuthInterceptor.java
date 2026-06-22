package com.skillmatch.ws;

import com.skillmatch.utils.JwtUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import static com.skillmatch.constants.RedisConstant.LOGIN_TOKEN_KEY;

/**
 * WebSocket 握手拦截器 — JWT 认证，与 HTTP TokenJWTInterceptor 逻辑一致
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket 握手失败: token 为空");
                return false;
            }
            try {
                String userId = JwtUtil.parseToken(token).get("userId");
                if (userId == null || userId.isEmpty()) {
                    return false;
                }
                String cached = redisTemplate.opsForValue().get(LOGIN_TOKEN_KEY + userId);
                if (cached == null || !cached.equals(token)) {
                    log.warn("WebSocket 握手失败: token 无效, userId={}", userId);
                    return false;
                }
                attributes.put("userId", userId);
                log.info("WebSocket 握手成功: userId={}", userId);
                return true;
            } catch (Exception e) {
                log.warn("WebSocket 握手失败: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
    }
}

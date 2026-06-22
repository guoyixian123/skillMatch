package com.skillmatch.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器 — 维护 userId → session 映射
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(String userId, WebSocketSession session) {
        sessions.put(userId, session);
        log.info("WebSocket 连接: userId={}, 当前在线: {}", userId, sessions.size());
    }

    public void remove(String userId) {
        sessions.remove(userId);
        log.info("WebSocket 断开: userId={}, 当前在线: {}", userId, sessions.size());
    }

    public WebSocketSession get(String userId) {
        return sessions.get(userId);
    }

    public boolean isOnline(String userId) {
        return sessions.containsKey(userId);
    }

    public int onlineCount() {
        return sessions.size();
    }
}

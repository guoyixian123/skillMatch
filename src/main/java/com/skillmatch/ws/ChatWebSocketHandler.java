package com.skillmatch.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * 聊天 WebSocket 处理器
 *
 * 消息格式（JSON）:
 *   { "type": "chat", "messageId": "xxx", "fromUserId": "xxx",
 *     "fromName": "张三", "content": "你好", "createdAt": "..." }
 *   { "type": "ping" }   ← 心跳，服务端返回 pong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        if (userId != null) {
            sessionManager.register(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = MAPPER.readValue(payload, Map.class);
        String type = (String) msg.get("type");

        if ("ping".equals(type)) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        if (userId != null) {
            sessionManager.remove(userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输错误: {}", exception.getMessage());
        String userId = getUserId(session);
        if (userId != null) {
            sessionManager.remove(userId);
        }
    }

    private String getUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }
}

package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.SendMessageDTO;
import com.skillmatch.domain.po.ChatMessage;
import com.skillmatch.domain.vo.ChatMessageVO;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.ChatMessageMapper;
import com.skillmatch.service.IChatService;
import com.skillmatch.service.IFriendService;
import com.skillmatch.ws.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatService {

    private final IFriendService friendService;
    private final WebSocketSessionManager sessionManager;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public ChatMessageVO sendMessage(SendMessageDTO dto) {
        String fromUserId = UserContext.getUserId();

        if (dto.getToUserId() == null || dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息内容不能为空");
        }
        if (dto.getContent().length() > 2000) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息内容不能超过2000字");
        }
        if (!friendService.isFriend(fromUserId, dto.getToUserId())) {
            throw new BusinessException(ErrorCode.NOT_AUTH, "你们还不是好友");
        }
        //不能给自己发消息
        if (fromUserId.equals(dto.getToUserId())) {
            throw new BusinessException(ErrorCode.NOT_AUTH, "不能给自己发送消息");
        }
        ChatMessage msg = new ChatMessage()
                .setFromUserId(fromUserId)
                .setToUserId(dto.getToUserId())
                .setContent(dto.getContent())
                .setIsRead(false)
                .setCreatedAt(LocalDateTime.now());
        save(msg);

        // WebSocket 实时推送到接收方
        WebSocketSession recipientSession = sessionManager.get(dto.getToUserId());
        log.info("WS push: toUserId={}, session={}, onlineCount={}",
                dto.getToUserId(), recipientSession != null, sessionManager.onlineCount());
        if (recipientSession != null && recipientSession.isOpen()) {
            try {
                Map<String, Object> pushMsg = new LinkedHashMap<>();
                pushMsg.put("type", "chat");
                pushMsg.put("messageId", msg.getId());
                pushMsg.put("fromUserId", fromUserId);
                pushMsg.put("content", dto.getContent());
                pushMsg.put("createdAt", msg.getCreatedAt().toString());
                recipientSession.sendMessage(new TextMessage(MAPPER.writeValueAsString(pushMsg)));
                log.info("WS push OK: {} → {}", fromUserId, dto.getToUserId());
            } catch (Exception e) {
                log.warn("WS push 失败: {}", e.getMessage());
            }
        } else {
            log.info("WS push 跳过: 对方不在线 (session={})", recipientSession != null);
        }

        ChatMessageVO vo = new ChatMessageVO();
        BeanUtil.copyProperties(msg, vo);
        return vo;
    }

    @Override
    public List<ChatMessageVO> getMessages(String friendId, String afterId, Integer limit) {
        String userId = UserContext.getUserId();
        if (!friendService.isFriend(userId, friendId)) {
            throw new BusinessException(ErrorCode.NOT_AUTH, "你们还不是好友");
        }
        if (limit == null || limit <= 0) limit = 50;

        List<ChatMessage> messages = baseMapper.selectMessages(userId, friendId, afterId, limit);
        return messages.stream().map(m -> {
            ChatMessageVO vo = new ChatMessageVO();
            BeanUtil.copyProperties(m, vo);
            return vo;
        }).toList();
    }

    @Override
    public void markAsRead(String friendId) {
        String userId = UserContext.getUserId();
        baseMapper.markAsRead(friendId, userId);
    }
}

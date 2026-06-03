package com.skillmatch.controller;

import com.skillmatch.domain.dto.SendMessageDTO;
import com.skillmatch.domain.vo.ChatMessageVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.IChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;

    @GetMapping("/messages/{friendId}")
    public RESTful<List<ChatMessageVO>> getMessages(@PathVariable String friendId,
                                                     @RequestParam(required = false) String afterId,
                                                     @RequestParam(required = false) Integer limit) {
        log.info("获取聊天记录, friendId={}", friendId);
        List<ChatMessageVO> messages = chatService.getMessages(friendId, afterId, limit);
        return RESTful.success(messages);
    }

    @PostMapping("/messages")
    public RESTful<ChatMessageVO> sendMessage(@Valid @RequestBody SendMessageDTO dto) {
        log.info("发送消息, toUserId={}", dto.getToUserId());
        ChatMessageVO vo = chatService.sendMessage(dto);
        return RESTful.success(vo);
    }

    @PutMapping("/messages/{friendId}/read")
    public RESTful<Void> markAsRead(@PathVariable String friendId) {
        log.info("标记已读, friendId={}", friendId);
        chatService.markAsRead(friendId);
        return RESTful.success(null);
    }
}

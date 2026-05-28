package com.skillmatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.dto.SendMessageDTO;
import com.skillmatch.domain.po.ChatMessage;
import com.skillmatch.domain.vo.ChatMessageVO;

import java.util.List;

public interface IChatService extends IService<ChatMessage> {

    ChatMessageVO sendMessage(SendMessageDTO dto);

    List<ChatMessageVO> getMessages(String friendId, String afterId, Integer limit);

    void markAsRead(String friendId);
}

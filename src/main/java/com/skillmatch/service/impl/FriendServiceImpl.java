package com.skillmatch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.po.ChatMessage;
import com.skillmatch.domain.po.Friend;
import com.skillmatch.domain.vo.FriendVO;
import com.skillmatch.mapper.ChatMessageMapper;
import com.skillmatch.mapper.FriendMapper;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.domain.vo.UserBasicVO;
import com.skillmatch.service.IFriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements IFriendService {

    private final UserMapper userMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    @Transactional
    public void addFriend(String userId, String friendId) {
        // 避免重复添加
        boolean exists = lambdaQuery()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .exists();
        if (exists) return;
        //不能自己添加自己
        if (userId.equals(friendId)) return;

        LocalDateTime now = LocalDateTime.now();
        Friend f1 = new Friend().setUserId(userId).setFriendId(friendId).setCreatedAt(now);
        Friend f2 = new Friend().setUserId(friendId).setFriendId(userId).setCreatedAt(now);
        saveBatch(List.of(f1, f2));
    }

    @Override
    public List<FriendVO> getFriends() {
        String userId = UserContext.getUserId();
        List<Friend> friends = lambdaQuery()
                .eq(Friend::getUserId, userId)
                .list();
        if (friends.isEmpty()) return List.of();

        List<FriendVO> result = new ArrayList<>(friends.size());
        for (Friend f : friends) {
            String friendId = f.getFriendId();
            UserBasicVO userBasic = userMapper.getUserBasicInfo(friendId);
            if (userBasic == null) continue;

            FriendVO vo = new FriendVO();
            vo.setUserId(userBasic.getId());
            vo.setName(userBasic.getName());
            vo.setAvatarUrl(userBasic.getAvatarUrl());

            // 最后一条消息
            ChatMessage lastMsg = chatMessageMapper.selectLastMessage(userId, friendId);
            if (lastMsg != null) {
                vo.setLastMessage(lastMsg.getContent());
                vo.setLastMessageTime(lastMsg.getCreatedAt());
            }

            // 未读数
            int unread = chatMessageMapper.countUnreadFrom(userId, friendId);
            vo.setUnreadCount(unread > 0 ? unread : null);

            result.add(vo);
        }

        // 按最后消息时间倒序
        result.sort((a, b) -> {
            if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        return result;
    }

    @Override
    @Transactional
    public void removeFriend(String friendId) {
        String userId = UserContext.getUserId();
        lambdaUpdate()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .remove();
        lambdaUpdate()
                .eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId)
                .remove();
    }

    @Override
    public Integer getUnreadCount() {
        String userId = UserContext.getUserId();
        return chatMessageMapper.countUnread(userId);
    }

    @Override
    public boolean isFriend(String userId, String friendId) {
        return lambdaQuery()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .exists();
    }
}

package com.skillmatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.po.Friend;
import com.skillmatch.domain.vo.FriendVO;

import java.util.List;

public interface IFriendService extends IService<Friend> {

    void addFriend(String userId, String friendId);

    List<FriendVO> getFriends();

    void removeFriend(String friendId);

    Integer getUnreadCount();

    boolean isFriend(String userId, String friendId);
}

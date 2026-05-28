package com.skillmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skillmatch.domain.po.ChatMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("<script>" +
            "SELECT * FROM chat_message WHERE " +
            "((from_user_id = #{userId} AND to_user_id = #{friendId}) OR " +
            "(from_user_id = #{friendId} AND to_user_id = #{userId})) " +
            "<if test='afterId != null'>AND id &gt; #{afterId} </if>" +
            "ORDER BY created_at ASC LIMIT #{limit}" +
            "</script>")
    List<ChatMessage> selectMessages(@Param("userId") String userId,
                                     @Param("friendId") String friendId,
                                     @Param("afterId") String afterId,
                                     @Param("limit") Integer limit);

    @Update("UPDATE chat_message SET is_read = 1 WHERE from_user_id = #{fromUserId} AND to_user_id = #{toUserId} AND is_read = 0")
    int markAsRead(@Param("fromUserId") String fromUserId, @Param("toUserId") String toUserId);

    @Select("SELECT COUNT(*) FROM chat_message WHERE to_user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") String userId);

    @Select("SELECT * FROM chat_message WHERE " +
            "((from_user_id = #{userId} AND to_user_id = #{friendId}) OR " +
            "(from_user_id = #{friendId} AND to_user_id = #{userId})) " +
            "ORDER BY created_at DESC LIMIT 1")
    ChatMessage selectLastMessage(@Param("userId") String userId, @Param("friendId") String friendId);

    @Select("SELECT COUNT(*) FROM chat_message WHERE from_user_id = #{friendId} AND to_user_id = #{userId} AND is_read = 0")
    int countUnreadFrom(@Param("userId") String userId, @Param("friendId") String friendId);
}

package com.skillmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skillmatch.domain.po.ChatMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

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

    /**
     * 批量查每个好友的最后一条消息（替代N+1逐条查询）
     * 返回Map：key=fromUserId, value=ChatMessage
     */
    @Select("<script>" +
            "SELECT * FROM chat_message WHERE id IN (" +
            "  SELECT MAX(id) FROM chat_message WHERE (" +
            "    (from_user_id = #{userId} AND to_user_id IN " +
            "      <foreach collection='friendIds' item='fid' open='(' separator=',' close=')'>#{fid}</foreach>" +
            "    ) OR " +
            "    (from_user_id IN " +
            "      <foreach collection='friendIds' item='fid' open='(' separator=',' close=')'>#{fid}</foreach>" +
            "      AND to_user_id = #{userId}" +
            "    )" +
            "  ) GROUP BY CASE WHEN from_user_id = #{userId} THEN to_user_id ELSE from_user_id END" +
            ") ORDER BY created_at DESC" +
            "</script>")
    List<ChatMessage> selectLastMessageBatch(@Param("userId") String userId, @Param("friendIds") List<String> friendIds);

    /**
     * 批量查每个好友的未读消息数（替代N+1逐条查询）
     * 返回List<Map>：每条记录包含 fromUserId 和 cnt
     */
    @Select("<script>" +
            "SELECT from_user_id AS fromUserId, COUNT(*) AS cnt FROM chat_message " +
            "WHERE to_user_id = #{userId} AND is_read = 0 AND from_user_id IN " +
            "<foreach collection='friendIds' item='fid' open='(' separator=',' close=')'>#{fid}</foreach>" +
            " GROUP BY from_user_id" +
            "</script>")
    List<Map<String, Object>> countUnreadFromBatch(@Param("userId") String userId, @Param("friendIds") List<String> friendIds);
}

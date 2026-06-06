package com.skillmatch.mapper;

import com.skillmatch.domain.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skillmatch.domain.vo.UserBasicVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
public interface UserMapper extends BaseMapper<User> {


    /**
     * 保存用户点赞数
     */
    @Update("update user set like_count = like_count + 1 where user_id = #{bizId}")
    void saveLikeCount(String bizId);

    /**
     * 用户点赞数 -1（取消点赞时调用）
     * 使用 GREATEST 函数保证点赞数不会低于 0
     */
    @Update("update user set like_count = greatest(like_count - 1, 0) where user_id = #{bizId}")
    void removeLikeCount(String bizId);

    /**
     * 获取用户基本信息
     */
    @Select("select user_id as id,name,head_url as avatarUrl from user where user_id=#{userId} LIMIT 1")
    UserBasicVO getUserBasicInfo(String userId);

    /**
     * 获取用户点赞数
     */
    @Select("select like_count from user where user_id = #{bizId}")
    Integer selectLikeCount(String bizId);

    /**
     * 用户帖子数 +1
     */
    @Update("update user set post_count = post_count + 1 where user_id = #{userId}")
    void incrPostCount(String userId);

    /**
     * 用户帖子数 -1（使用 GREATEST 防止并发导致负数）
     */
    @Update("update user set post_count = greatest(post_count - 1, 0) where user_id = #{userId}")
    void decrPostCount(String userId);

    /**
     * 批量获取用户基本信息（替代N+1逐条查询）
     */
    @Select("<script>" +
            "SELECT user_id AS id, name, head_url AS avatarUrl FROM user WHERE user_id IN " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>" +
            "#{uid}" +
            "</foreach>" +
            "</script>")
    List<UserBasicVO> getUserBasicInfoBatch(@Param("userIds") List<String> userIds);
}

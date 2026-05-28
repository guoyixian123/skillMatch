package com.skillmatch.mapper;

import com.skillmatch.domain.po.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Speed
 * @since 2026-05-22
 */
public interface PostMapper extends BaseMapper<Post> {
@Update("update post set like_count = like_count + 1 where id = #{bizId}")
    void saveLikeCount(String bizId);

    /**
     * 帖子点赞数 -1（取消点赞时调用）
     * 使用 GREATEST 函数保证点赞数不会低于 0，防止并发取消导致负数
     */
    @Update("update post set like_count = greatest(like_count - 1, 0) where id = #{bizId}")
    void removeLikeCount(String bizId);

@Select("select like_count from post where id = #{bizId}")
    Integer selectLikeCount(String bizId);

    /**
     * 查询帖子作者ID，用于点赞通知时确定接收者
     */
    @Select("select author_id from post where id = #{postId}")
    String selectAuthorId(String postId);
}

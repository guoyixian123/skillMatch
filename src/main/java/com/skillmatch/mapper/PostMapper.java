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

@Select("select like_count from post where id = #{bizId}")
    int selectLikeCount(String bizId);
}

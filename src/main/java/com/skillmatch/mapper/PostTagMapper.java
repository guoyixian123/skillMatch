package com.skillmatch.mapper;

import com.skillmatch.domain.po.PostTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Speed
 * @since 2026-05-22
 */
public interface PostTagMapper extends BaseMapper<PostTag> {
@Select("SELECT tag_name FROM post_tag WHERE post_id = #{id}")
    List<String> selectTagsByPostId(String id);
}

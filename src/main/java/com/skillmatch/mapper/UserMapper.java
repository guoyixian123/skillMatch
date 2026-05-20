package com.skillmatch.mapper;

import com.skillmatch.domain.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
public interface UserMapper extends BaseMapper<User> {
@Select("select head_url from user where user_id = #{userId}")
    String selectAvatarOld(String userId);
}

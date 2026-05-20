package com.skillmatch.mapper;

import com.skillmatch.domain.po.UserGallery;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Speed
 * @since 2026-05-20
 */
@Mapper
public interface UserGalleryMapper extends BaseMapper<UserGallery> {
    @Select("select max(sort_order) from user_gallery")
    Integer selectMaxSort();
    @Select("select image_url from user_gallery where id = #{imageId}")
    String selectUrl(String imageId);
}

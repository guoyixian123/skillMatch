package com.skillmatch.service;

import com.skillmatch.domain.po.UserGallery;
import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.vo.GalleryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Speed
 * @since 2026-05-20
 */
public interface IUserGalleryService extends IService<UserGallery> {

    List<GalleryVO> getUserGalleryInfo();

    GalleryVO uploadUserGallery(MultipartFile file);

    void removeImageById(String imageId);
}

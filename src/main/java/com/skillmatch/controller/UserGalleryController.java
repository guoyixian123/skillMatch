package com.skillmatch.controller;


import com.skillmatch.domain.vo.GalleryVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.IUserGalleryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Speed
 * @since 2026-05-20
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserGalleryController {
    private final IUserGalleryService userGalleryService;
    /**
     * 获取用户照片信息
     */
    @GetMapping("/gallery")
    public RESTful<List<GalleryVO>> getUserGallery() {
        log.info("获取用户照片信息");
       List<GalleryVO> galleryList=userGalleryService.getUserGalleryInfo();
        return RESTful.success(galleryList);
    }
    /**
     * 添加用户照片
     */
    @PostMapping("/gallery")
    public RESTful<GalleryVO> addUserGallery(@RequestParam MultipartFile file) {
        log.info("添加用户照片");
        GalleryVO galleryVO=userGalleryService.uploadUserGallery(file);
        return RESTful.success(galleryVO, "上传成功");
    }
    /**
     * 删除用户照片
     */
    @DeleteMapping("/gallery/{imageId}")
    public RESTful<Void> deleteUserGallery(@PathVariable String imageId) {
        log.info("删除用户照片");
        userGalleryService.removeImageById(imageId);
        return RESTful.success(null, "已删除");
    }
}

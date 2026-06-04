package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.aliyuncs.exceptions.ClientException;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.po.UserGallery;
import com.skillmatch.domain.vo.GalleryVO;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.UserGalleryMapper;
import com.skillmatch.service.IUserGalleryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.utils.OssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Speed
 * @since 2026-05-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserGalleryServiceImpl extends ServiceImpl<UserGalleryMapper, UserGallery> implements IUserGalleryService {
    private final UserGalleryMapper userGalleryMapper;

    /**
     * 获取用户图片信息
     */
    @Override
    public List<GalleryVO> getUserGalleryInfo() {
        String userId = UserContext.getUserId();
        //查询用户图片
        List<UserGallery> list = lambdaQuery()
                .eq(UserGallery::getUserId, userId)
                .list();
        if (list.isEmpty()) return Collections.emptyList();
        //拷贝数据到VO
        List<GalleryVO> galleryVOS = BeanUtil.copyToList(list, GalleryVO.class);
        //排序
        galleryVOS.sort(Comparator.comparing(GalleryVO::getSortOrder));
        //返回VO
        return galleryVOS;
    }

    /**
     * 上传用户图片
     */
    @Override
    public GalleryVO uploadUserGallery(MultipartFile file) {
        if (file == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择上传文件");
        }
        //上传用户展示照片,获取出照片url
        try {
            String userGallery = OssUtil.upload(file.getBytes(), file.getOriginalFilename(), "userGallery");
            //保存到数据库
            //1 查询表中sort自动中最大值,默认为1
            Integer maxSort = userGalleryMapper.selectMaxSort();
            //2 封装数据,保存到数据库
            UserGallery gallery = new UserGallery();
            gallery.setUserId(UserContext.getUserId());
            gallery.setImageUrl(userGallery);
            gallery.setCreatedAt(LocalDateTime.now());
            gallery.setSortOrder(maxSort == null ? 1 : maxSort + 1);
            save(gallery);
            //构建返回数据vo
            GalleryVO galleryVO = new GalleryVO();
            galleryVO.setId(gallery.getId());
            galleryVO.setImageUrl(gallery.getImageUrl());
            galleryVO.setSortOrder(gallery.getSortOrder());
            return galleryVO;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "上传图片失败");
        }
    }

    /**
     * 删除用户照片
     */
    @Override
    public void removeImageById(String imageId) {
        String userId = UserContext.getUserId();
        UserGallery byId = getById(imageId);
        if (byId == null) throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        //判断图片是否属于当前用户
        if(!byId.getUserId().equals(userId)){
            throw new BusinessException(ErrorCode.NOT_AUTH, "不能删除他人的图片");
        }
        String url = userGalleryMapper.selectUrl(imageId);
        if (url == null || url.isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        //删除数据库中的图片
        removeById(imageId);
        //删除OSS中的图片文件
        try {
            OssUtil.delete(url);
        } catch (ClientException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "删除图片失败");
        }
    }
}

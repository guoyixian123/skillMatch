package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.SecureUtil;
import com.skillmatch.context.BeanContext;
import com.skillmatch.domain.dto.PassWordDTO;
import com.skillmatch.domain.dto.UserDTO;
import com.skillmatch.domain.po.User;
import com.skillmatch.domain.vo.UserVO;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.utils.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
/*
    查询用户信息
 */
    @Override
    public UserVO getProfile(String userId) {
        if(userId == null||userId.isEmpty()){
            throw new RuntimeException("用户不存在");
        }
        User user = getById(userId);
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        if(user == null){
            throw new RuntimeException("用户不存在");
        }
        return userVO;
    }
/*
    修改用户信息
 */
    @Override
    public void updateUser(UserDTO userInfo) {
        if(userInfo == null) return;
        //获取当前用户
        String userId = BeanContext.getUserId();
        if(userId == null||userId.isEmpty()) return;
        boolean update = lambdaUpdate()
                .eq(User::getUserId, userId)
                .set(User::getName, userInfo.getName())
                .set(User::getSignature, userInfo.getSignature())
                .set(User::getBio, userInfo.getBio())
                .set(User::getContactInfo, userInfo.getContactInfo())
                .update();
        if(!update){
            throw new RuntimeException("修改用户信息失败");
        }
        log.info("修改用户:id为{}信息成功", userId);
    }

    @Override
    public void uploadAvatar(MultipartFile avatarUrl){
        //上传用户头像
        //TODO: 可能存在oss容器中一个用户多个头像的情况,在更新新头像时,先删除旧的头像,日后处理
        try {
            String avatarNewUrl = OssUtil.upload(avatarUrl.getBytes(), Objects.requireNonNull(avatarUrl.getOriginalFilename()), "avatar");
            boolean update = lambdaUpdate()
                    .eq(User::getUserId, BeanContext.getUserId())
                    .set(User::getAvatarUrl, avatarNewUrl)
                    .update();
            if(!update){
                throw new RuntimeException("修改用户头像失败");
            }
            log.info("用户头像更新头像成功:{}", avatarNewUrl);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败");
        }
    }

    @Override
    public void updatePassword(PassWordDTO password) {
        //获取当前用户
        String userId = BeanContext.getUserId();
        if(userId == null||userId.isEmpty()) return;
        //将密码进行md5处理
        String old = SecureUtil.md5(password.getOldPassword());
        String newPassword = SecureUtil.md5(password.getNewPassword());
        Long count = lambdaQuery()
                .eq(User::getPassword, old)
                .eq(User::getUserId, userId)
                .count();
        //TODO: 旧密码错误返回400,现在无法实现
        if(count == 0){
            throw new RuntimeException("旧密码错误");
        }
        boolean update = lambdaUpdate()
                .eq(User::getUserId, userId)
                .set(User::getPassword, newPassword)
                .update();
        if(!update){
            throw new RuntimeException("修改密码失败");
        }
        log.info("修改密码成功:{}", newPassword);
    }
}

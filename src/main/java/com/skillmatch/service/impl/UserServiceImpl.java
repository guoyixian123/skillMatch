package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.LocationDTO;
import com.skillmatch.domain.dto.PasswordDTO;
import com.skillmatch.domain.dto.UserDTO;
import com.skillmatch.domain.po.*;
import com.skillmatch.domain.vo.UserVO;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.NotificationMapper;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.utils.GeoUtil;
import com.skillmatch.utils.OssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

import static com.skillmatch.constants.RedisConstant.LOGIN_TOKEN_KEY;
import static com.skillmatch.constants.RedisConstant.USER_LOCATION_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    private final StringRedisTemplate redisTemplate;
    private final IPostService postService;
    private final IPostCommentService postCommentService;
    private final IPostTagService postTagService;
    private final ILikeInfoService likeInfoService;
    private final IContactRequestService contactRequestService;
    private final IUserSkillService userSkillService;
    private final IUserHobbyService userHobbyService;
    private final IUserGalleryService userGalleryService;
    private final INotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final GeoUtil geoUtil;

    /*
        查询用户信息
     */
    @Override
    public UserVO getProfile(String profileUserId) {
        String currentUserId = UserContext.getUserId();
        User target = lambdaQuery().eq(User::getUserId, profileUserId).one();
        if (target == null) return null;

        UserVO vo = new UserVO();
        // ... 设置 name, avatarUrl, signature, bio 等公共字段 ...
        BeanUtil.copyProperties(target, vo);
        // 只有本人或双方已接受交换请求时才返回联系方式
        boolean isSelf = currentUserId != null && currentUserId.equals(profileUserId);
        boolean canView = isSelf;
        if (!isSelf && currentUserId != null) {
            Long accepted = contactRequestService.lambdaQuery()
                    .and(w -> w
                            .eq(ContactRequest::getFromUserId, currentUserId)
                            .eq(ContactRequest::getToUserId, profileUserId)
                            .or()
                            .eq(ContactRequest::getFromUserId, profileUserId)
                            .eq(ContactRequest::getToUserId, currentUserId))
                    .eq(ContactRequest::getStatus, 2)  // 已接受
                    .count();
            canView = accepted > 0;
        }
        if (canView) {
            vo.setContactInfo(target.getContactInfo());
        }
        return vo;
    }

    /*
        修改用户信息
     */
    @Override
    public void updateUser(UserDTO userInfo) {
        if (userInfo == null) throw new BusinessException(ErrorCode.PARAM_ERROR);
        //获取当前用户
        String userId = UserContext.getUserId();
        //更新用户信息
        boolean update = lambdaUpdate()
                .eq(User::getUserId, userId)
                .set(StrUtil.isNotEmpty(userInfo.getName()),User::getName, userInfo.getName())
                .set(StrUtil.isNotEmpty(userInfo.getSignature()),User::getSignature, userInfo.getSignature())
                .set(StrUtil.isNotEmpty(userInfo.getBio()) ,  User::getBio, userInfo.getBio())
                .set(StrUtil.isNotEmpty(userInfo.getContactInfo()),User::getContactInfo, userInfo.getContactInfo())
                .update();
        //判断更新是否成功
        if (!update) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "修改用户信息失败");
        }
        log.info("修改用户:id为{}信息成功", userId);
    }

    /**
     * 上传用户头像
     */
    @Override
    public String uploadAvatar(MultipartFile avatarUrl) {
        //上传用户头像
        try {
            //TODO: 可能存在oss容器中一个用户多个头像的情况,在更新新头像时,先删除旧的头像,日后处理(y)
            //查询数据库中有没有头像的url
            User byId = getById(UserContext.getUserId());
            if(byId == null ){
                throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
            }
            String url = byId.getAvatarUrl();
            if (url != null) {
                //删除旧头像
                OssUtil.delete(url);
            }
            //上传用户头像
            String avatarNewUrl = OssUtil.upload(avatarUrl.getBytes(), Objects.requireNonNull(avatarUrl.getOriginalFilename()), "avatar");
            //更新数据库信息,覆盖旧的信息
            boolean update = lambdaUpdate()
                    .eq(User::getUserId, UserContext.getUserId())
                    .set(User::getAvatarUrl, avatarNewUrl)
                    .update();
            if (!update) {
                throw new BusinessException(ErrorCode.SERVER_ERROR, "修改用户头像失败");
            }
            log.info("用户头像更新头像成功:{}", avatarNewUrl);
            return avatarNewUrl;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    /**
     * 修改用户密码
     */
    @Override
    public void updatePassword(PasswordDTO password) {
        //获取当前用户
        String userId = UserContext.getUserId();
        //将密码进行md5处理
        String old = SecureUtil.md5(password.getOldPassword());
        String newPassword = SecureUtil.md5(password.getNewPassword());
        //查询数据库中密码,判断输入的密码是否正确
        Long count = lambdaQuery()
                .eq(User::getPassword, old)
                .eq(User::getUserId, userId)
                .count();
        //TODO: 旧密码错误返回400,现在无法实现(y)
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码错误");
        }
        //密码正确,更新数据库中的密码
        boolean update = lambdaUpdate()
                .eq(User::getUserId, userId)
                .set(User::getPassword, newPassword)
                .update();
        if (!update) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "修改密码失败");
        }
        log.info("修改密码成功:{}", newPassword);
    }

    /**
     * 修改用户地理位置
     */
    @Override
    @Transactional//修改数据库和redis需要添加事务
    public void updateLocation(LocationDTO location) {
        //参数校验
        if (location == null) throw new BusinessException(ErrorCode.PARAM_ERROR);
        String userId = UserContext.getUserId();
        String city = geoUtil.resolveCity(location.getLongitude(), location.getLatitude());

        // 在数据库中更新用户地理位置
        boolean update = lambdaUpdate()
                .eq(User::getUserId, userId)
                .set(User::getLatitude, location.getLatitude())
                .set(User::getLongitude, location.getLongitude())
                .set(User::getCity, city)//更新城市
                .update();

        //判断更新是否成功
        if (!update) {
            log.warn("用户地理位置更新失败:{}", userId);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "用户地理位置更新失败");
        }

        // 在redis中更新用户地理位置
        redisTemplate.opsForGeo().add(USER_LOCATION_KEY, new Point(location.getLongitude(), location.getLatitude()), userId);

    }

    @Override
    @Transactional
    public void removeBotInfo() {
        // 1. 获取所有机器人ID (avatarUrl 为空的用户)
        List<String> botIds = lambdaQuery()
                .eq(User::getRobot, true)
                .list()
                .stream()
                .map(User::getUserId)
                .toList();

        if (botIds.isEmpty()) {
            log.info("没有需要清理的BOT用户");
            return;
        }

        // 2. 查询这些用户的所有帖子 ID
        List<String> postIds = postService.lambdaQuery()
                .in(Post::getAuthorId, botIds)
                .list()
                .stream()
                .map(Post::getId)
                .toList();

        // 3. 级联删除帖子子表（post_tag、post_comment、like_info 中关联这些帖子的记录）
        if (!postIds.isEmpty()) {
            postTagService.lambdaUpdate().in(PostTag::getPostId, postIds).remove();
            postCommentService.lambdaUpdate().in(PostComment::getPostId, postIds).remove();
            likeInfoService.lambdaUpdate().in(LikeInfo::getBizId, postIds).remove();
        }

        // 4. 删除帖子主表
        postService.lambdaUpdate().in(Post::getAuthorId, botIds).remove();

        // 5. 删除用户维度的关联数据
        postCommentService.lambdaUpdate().in(PostComment::getUserId, botIds).remove();
        likeInfoService.lambdaUpdate().in(LikeInfo::getUserId, botIds).remove();
        notificationMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                .in(Notification::getActorId, botIds)
                .or()
                .in(Notification::getReceiverId, botIds));
        contactRequestService.lambdaUpdate().in(ContactRequest::getFromUserId, botIds).remove();
        contactRequestService.lambdaUpdate().in(ContactRequest::getToUserId, botIds).remove();
        userSkillService.lambdaUpdate().in(UserSkill::getUserId, botIds).remove();
        userHobbyService.lambdaUpdate().in(UserHobby::getUserId, botIds).remove();
        userGalleryService.lambdaUpdate().in(UserGallery::getUserId, botIds).remove();

        // 6. 删除 user 主表
        lambdaUpdate().in(User::getUserId, botIds).remove();

        // 7. 清理 Redis（GEO 位置 + login token）
        for (String botId : botIds) {
            redisTemplate.opsForGeo().remove(USER_LOCATION_KEY, botId);
            redisTemplate.delete(LOGIN_TOKEN_KEY + botId);
        }

        log.info("已清理 {} 个BOT用户及所有关联数据, botIds={}", botIds.size(), botIds);
    }
}

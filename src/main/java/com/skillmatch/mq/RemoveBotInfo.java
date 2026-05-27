package com.skillmatch.mq;

import com.skillmatch.constants.MqConstants;
import com.skillmatch.constants.RedisConstant;
import com.skillmatch.domain.po.*;
import com.skillmatch.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RemoveBotInfo {
    private final IUserService userService;
    private final IUserSkillService userSkillService;
    private final IUserHobbyService userHobbyService;
    private final IUserGalleryService userGalleryService;
    private final ILikeInfoService likeInfoService;
    private final IPostService postService;
    private final IPostCommentService postCommentService;
    private final IPostTagService postTagService;
    private final IContactRequestService contactRequestService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 删除 bot 用户的所有关联数据（DB + Redis）
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.REMOVE_BOT_QUEUE),
            exchange = @Exchange(value = MqConstants.REMOVE_BOT_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.REMOVE_BOT_ROUTING_KEY))
    @Transactional
    public void removeBotInfo(List<String> botIds) {
        if (botIds == null || botIds.isEmpty()) {
            return;
        }

        // 1. 查这些用户的所有帖子 ID（用于级联删除子表）
        List<String> postIds = postService.lambdaQuery()
                .in(Post::getAuthorId, botIds)
                .list()
                .stream()
                .map(Post::getId)
                .toList();

        // 2. 级联删除帖子子表（post_tag、post_comment、like_info 中关联这些帖子的记录）
        if (!postIds.isEmpty()) {
            postTagService.lambdaUpdate()
                    .in(PostTag::getPostId, postIds)
                    .remove();
            postCommentService.lambdaUpdate()
                    .in(PostComment::getPostId, postIds)
                    .remove();
            likeInfoService.lambdaUpdate()
                    .in(LikeInfo::getBizId, postIds)
                    .remove();
        }

        // 3. 删除这些用户的帖子
        postService.lambdaUpdate()
                .in(Post::getAuthorId, botIds)
                .remove();

        // 4. 删除用户直接的关联数据
        postCommentService.lambdaUpdate()
                .in(PostComment::getUserId, botIds)
                .remove();
        likeInfoService.lambdaUpdate()
                .in(LikeInfo::getUserId, botIds)
                .remove();
        contactRequestService.lambdaUpdate()
                .in(ContactRequest::getFromUserId, botIds)
                .remove();
        contactRequestService.lambdaUpdate()
                .in(ContactRequest::getToUserId, botIds)
                .remove();
        userSkillService.lambdaUpdate()
                .in(UserSkill::getUserId, botIds)
                .remove();
        userHobbyService.lambdaUpdate()
                .in(UserHobby::getUserId, botIds)
                .remove();
        userGalleryService.lambdaUpdate()
                .in(UserGallery::getUserId, botIds)
                .remove();
        // 6. 删除 user 主表
        userService.lambdaUpdate()
                .in(User::getUserId, botIds)
                .remove();

        // 5. 删除 Redis 中的用户地理位置
        for (String botId : botIds){
            redisTemplate.opsForGeo()
                    .remove(RedisConstant.USER_LOCATION_KEY,botId);
        }
        // 5. 删除用户在redis中的token
        for(String botId:botIds){
            redisTemplate.delete(RedisConstant.LOGIN_TOKEN_KEY+botId);

        }
        log.info("已清理 {} 个 bot 用户的所有数据, botIds={}", botIds.size(), botIds);
    }
}

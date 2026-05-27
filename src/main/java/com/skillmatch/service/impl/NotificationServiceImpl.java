package com.skillmatch.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.po.Notification;
import com.skillmatch.domain.vo.LikeNotificationVO;
import com.skillmatch.domain.vo.PageVO;
import com.skillmatch.domain.vo.UserBasicVO;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.NotificationMapper;
import com.skillmatch.mapper.PostMapper;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 * 负责通知的增删查改以及 preview 摘要文案的组装
 *
 * @author Speed
 * @since 2026-05-27
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    private final UserMapper userMapper;
    private final PostMapper postMapper;

    /**
     * 保存一条通知记录
     * 初始状态为未读，创建时间取当前时间
     */
    @Override
    public void save(String actorId, String receiverId, Integer type, String bizId) {
        Notification n = new Notification()
                .setReceiverId(receiverId)
                .setActorId(actorId)
                .setType(type)
                .setBizId(bizId)
                .setIsRead(false)
                .setCreatedAt(LocalDateTime.now());
        save(n);
    }

    /**
     * 取消点赞时删除对应的通知
     * 根据点赞者、实体ID、类型三个条件精确定位要删除的通知
     */
    @Override
    public void removeByLike(String actorId, String bizId, Integer type) {
        lambdaUpdate()
                .eq(Notification::getActorId, actorId)
                .eq(Notification::getBizId, bizId)
                .eq(Notification::getType, type)
                .remove();
    }

    /**
     * 分页查询当前用户的通知列表
     * 按创建时间倒序排列
     * 批量查询点赞者信息和帖子标题，减少 N+1 查询
     */
    @Override
    public PageVO<LikeNotificationVO> list(int page, int size) {
        // 1. 分页查通知记录
        String userId = UserContext.getUserId();
        Page<Notification> result = lambdaQuery()
                .eq(Notification::getReceiverId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .page(new Page<>(page, size));
        List<Notification> records = result.getRecords();
        if (records.isEmpty()) {
            return PageVO.of(result, List.of());
        }

        // 2. 批量查询点赞者信息（id + 姓名 + 头像），避免逐条查库
        Set<String> actorIds = records.stream()
                .map(Notification::getActorId)
                .collect(Collectors.toSet());
        Map<String, UserBasicVO> actorMap = userMapper.selectBatchIds(actorIds).stream()
                .collect(Collectors.toMap(u -> u.getUserId().toString(),
                        u -> {
                            UserBasicVO vo = new UserBasicVO();
                            vo.setId(u.getUserId());
                            vo.setName(u.getName());
                            vo.setAvatarUrl(u.getAvatarUrl());
                            return vo;
                        }));

        // 3. 批量查询帖子标题（仅 type=2 帖子被赞的通知需要），用于组装 preview 文案
        Set<String> postIds = records.stream()
                .filter(n -> n.getType() == 2 && n.getBizId() != null)
                .map(Notification::getBizId)
                .collect(Collectors.toSet());
        Map<String, String> titleMap = postIds.isEmpty()
                ? Map.of()
                : postMapper.selectBatchIds(postIds).stream()
                        .collect(Collectors.toMap(p -> p.getId(), p -> p.getTitle()));

        // 4. 组装返回 VO，填充 actor 和 preview 字段
        List<LikeNotificationVO> vos = new ArrayList<>();
        for (Notification n : records) {
            LikeNotificationVO vo = new LikeNotificationVO();
            vo.setId(n.getId());
            vo.setType(n.getType());
            vo.setActor(actorMap.getOrDefault(n.getActorId(), new UserBasicVO()));
            vo.setBizId(n.getBizId());
            vo.setIsRead(n.getIsRead());
            vo.setPreview(buildPreview(n.getType(), n.getBizId(), titleMap));
            vo.setCreatedAt(n.getCreatedAt());
            vos.add(vo);
        }
        return PageVO.of(result, vos);
    }

    /**
     * 查询当前用户未读通知数量
     * 用于前端导航栏铃铛角标
     */
    @Override
    public Integer unreadCount() {
        String userId = UserContext.getUserId();
        return Math.toIntExact(lambdaQuery()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, false)
                .count());
    }

    /**
     * 标记单条通知为已读
     * 先校验通知存在，再校验通知属于当前用户，防止越权操作
     */
    @Override
    public void markRead(Long id) {
        Notification n = getById(id);
        if (n == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (!n.getReceiverId().equals(UserContext.getUserId()))
            throw new BusinessException(ErrorCode.NOT_AUTH);
        lambdaUpdate()
                .eq(Notification::getId, id)
                .set(Notification::getIsRead, true)
                .update();
    }

    /**
     * 一键已读：将当前用户所有未读通知标记为已读
     */
    @Override
    public void markAllRead() {
        String userId = UserContext.getUserId();
        lambdaUpdate()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true)
                .update();
    }

    /**
     * 组装通知摘要文案
     * type=1：固定文本"赞了你"
     * type=2：拼接帖子标题，超出20字截断
     */
    private String buildPreview(Integer type, String bizId, Map<String, String> titleMap) {
        return switch (type) {
            case 1 -> "赞了你";
            case 2 -> {
                String title = titleMap.getOrDefault(bizId, "");
                String shortTitle = title.length() > 20
                        ? title.substring(0, 20) + "..."
                        : title;
                yield "赞了你的帖子《" + shortTitle + "》";
            }
            default -> "新通知";
        };
    }
}

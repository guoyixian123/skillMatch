package com.skillmatch.service.impl;

import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.LikeDTO;
import com.skillmatch.domain.po.LikeInfo;
import com.skillmatch.domain.vo.LikeVO;
import com.skillmatch.mapper.LikeInfoMapper;
import com.skillmatch.mapper.PostMapper;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.service.ILikeInfoService;
import com.skillmatch.service.INotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Speed
 * @since 2026-05-21
 */
@Service
@RequiredArgsConstructor
public class LikeInfoServiceImpl extends ServiceImpl<LikeInfoMapper, LikeInfo> implements ILikeInfoService {
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final INotificationService notificationService;

    @Override
    public LikeVO saveLikeInfo(LikeDTO likeDTO) {
        String userId = UserContext.getUserId();
        //判断是否重复点赞,查询记录信息->点赞数据库
        Long count = lambdaQuery()
                .eq(LikeInfo::getUserId, userId)
                .eq(LikeInfo::getBizId, likeDTO.getBizId())
                .count();
        if (count > 0) {
            throw new RuntimeException("请勿重复点赞");
        }
        //不能给自己点赞
        if (userId.equals(likeDTO.getBizId())) {
           throw new RuntimeException("不能给自己点赞");
        }
        //保存点赞信息
        LikeInfo likeInfo = new LikeInfo().setBizId(likeDTO.getBizId()).setUserId(userId).setType(likeDTO.getType());
        save(likeInfo);
        //对应业务点赞数+1,落库
        LikeVO vo = saveLikeCount(likeDTO);
        //发送点赞通知
        sendLikeNotification(userId, likeDTO);
        return vo;
    }

    private LikeVO saveLikeCount(LikeDTO likeDTO) {
        int count = switch (likeDTO.getType()) {
            case 1 -> {
                userMapper.saveLikeCount(likeDTO.getBizId());
                yield userMapper.selectLikeCount(likeDTO.getBizId());
            }
            case 2 -> {
                postMapper.saveLikeCount(likeDTO.getBizId());
                yield postMapper.selectLikeCount(likeDTO.getBizId());
            }
            default -> 0;
        };
        return new LikeVO().setLikeCount(count).setIsLiked(true);
    }

    @Override
    public LikeVO removeLikeInfo(LikeDTO likeDTO) {
        String userId = UserContext.getUserId();
        // 查点赞记录
        LikeInfo record = lambdaQuery()
                .eq(LikeInfo::getUserId, userId)
                .eq(LikeInfo::getBizId, likeDTO.getBizId())
                .eq(LikeInfo::getType, likeDTO.getType())
                .one();
        if (record == null) {
            throw new RuntimeException("还未点赞，无法取消");
        }
        removeById(record.getId());
        //删除对应通知
        notificationService.removeByLike(userId, likeDTO.getBizId(), likeDTO.getType());
        return removeLikeCount(likeDTO);
    }

    private LikeVO removeLikeCount(LikeDTO likeDTO) {
        int count = switch (likeDTO.getType()) {
            case 1 -> {
                userMapper.removeLikeCount(likeDTO.getBizId());
                yield userMapper.selectLikeCount(likeDTO.getBizId());
            }
            case 2 -> {
                postMapper.removeLikeCount(likeDTO.getBizId());
                yield postMapper.selectLikeCount(likeDTO.getBizId());
            }
            default -> 0;
        };
        return new LikeVO().setLikeCount(count).setIsLiked(false);
    }

    /**
     * 发送点赞通知给被点赞者
     * <p>
     * 根据点赞类型确定接收者：
     * type=1（个人主页被赞）→ bizId 直接就是被点赞用户ID
     * type=2（帖子被赞）→ 通过 postMapper 查询帖子作者ID
     * <p>
     * 自己给自己点赞不会产生通知，避免无效数据
     *
     * @param actorId 点赞者用户ID（当前登录用户）
     * @param likeDTO 点赞数据，包含 type 和 bizId
     */
    private void sendLikeNotification(String actorId, LikeDTO likeDTO) {
        String receiverId;
        if (likeDTO.getType() == 1) {
            // type=1: bizId 就是被点赞的用户ID，直接作为接收者
            receiverId = likeDTO.getBizId();
        } else {
            // type=2: bizId 是帖子ID，需要查询帖子作者作为通知接收者
            receiverId = postMapper.selectAuthorId(likeDTO.getBizId());
        }
        // 不通知自己（自己给自己点赞已在 saveLikeInfo 中拦截，此处为双重保障）
        if (receiverId != null && !receiverId.equals(actorId)) {
            notificationService.save(actorId, receiverId, likeDTO.getType(), likeDTO.getBizId());
        }
    }
}

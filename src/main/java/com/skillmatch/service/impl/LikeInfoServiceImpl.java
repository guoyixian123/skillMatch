package com.skillmatch.service.impl;

import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.LikeDTO;
import com.skillmatch.domain.po.LikeInfo;
import com.skillmatch.domain.vo.LikeVO;
import com.skillmatch.mapper.LikeInfoMapper;
import com.skillmatch.mapper.PostMapper;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.service.ILikeInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
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
       return saveLikeCount(likeDTO);
    }

    private LikeVO saveLikeCount(LikeDTO likeDTO) {
        //获取点赞数
        int count = switch (likeDTO.getType()) {//ZY(个人主页)1/SQ(社区)2
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
        return new LikeVO().setLikeCount(count);
    }
}

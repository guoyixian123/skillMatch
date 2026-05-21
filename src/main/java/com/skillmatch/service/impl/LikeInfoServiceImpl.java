package com.skillmatch.service.impl;

import com.skillmatch.context.BeanContext;
import com.skillmatch.domain.dto.LikeDTO;
import com.skillmatch.domain.po.LikeInfo;
import com.skillmatch.domain.vo.LikeVO;
import com.skillmatch.mapper.LikeInfoMapper;
import com.skillmatch.service.ILikeInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.skillmatch.constants.RedisConstant.COUNT_LIKE_KEY;
import static com.skillmatch.constants.RedisConstant.RECODE_LIKE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Speed
 * @since 2026-05-21
 */
@Service
@RequiredArgsConstructor
public class LikeInfoServiceImpl extends ServiceImpl<LikeInfoMapper, LikeInfo> implements ILikeInfoService {
private final StringRedisTemplate redisTemplate;
    @Override
    public LikeVO saveLikeInfo(LikeDTO likeDTO) {
        String userId = BeanContext.getUserId();
        //判断是否已经点赞
        if(likeDTO.getIsLike()){//点赞
            return null;
        }
        //是否重复点赞
        //1.先查redis中是否存在
        Boolean member = redisTemplate.opsForSet().isMember(RECODE_LIKE_KEY + likeDTO.getBiz_id(), userId);
        if(member == null || !member){
            //2.不存在则查数据库中
            Long count = lambdaQuery()
                    .eq(LikeInfo::getUserId, userId)
                    .eq(LikeInfo::getBizId, likeDTO.getBiz_id())
                    .count();
            if(count != 0){
                //数据库有数据,加入redis中
                redisTemplate.opsForSet().add(RECODE_LIKE_KEY + likeDTO.getBiz_id(), userId);
                //返回
                return null;
            }
        }
        //3.不存在则插入redis中
        redisTemplate.opsForSet().add(RECODE_LIKE_KEY + likeDTO.getBiz_id(), userId);
        //4.增加点赞数
        //1.查redis中是否存在
        redisTemplate.opsForZSet().score(COUNT_LIKE_KEY, likeDTO.getBiz_id());
        redisTemplate.opsForZSet().add(COUNT_LIKE_KEY, likeDTO.getBiz_id(), 1);

    }
}

package com.skillmatch.service;

import com.skillmatch.domain.dto.LikeDTO;
import com.skillmatch.domain.po.LikeInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.vo.LikeVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Speed
 * @since 2026-05-21
 */
public interface ILikeInfoService extends IService<LikeInfo> {

    LikeVO saveLikeInfo(LikeDTO likeDTO);

}

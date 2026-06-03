package com.skillmatch.controller;


import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.LikeDTO;
import com.skillmatch.domain.vo.LikeVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.ILikeInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Speed
 * @since 2026-05-21
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LikeInfoController {
    private final ILikeInfoService likeInfoService;
    @PostMapping("/like")
    public RESTful<LikeVO> like(@Valid @RequestBody LikeDTO likeDTO) {
        log.info("用户:{}点赞了:{}", UserContext.getUserId(), likeDTO.getBizId());
        LikeVO likeVO = likeInfoService.saveLikeInfo(likeDTO);
        return RESTful.success(likeVO, "点赞成功");
    }

    /**
     * 取消点赞
     * 删除点赞记录、点赞数 -1、同时删除对应的通知记录
     * 与点赞接口使用同一路径 /api/like，通过 HTTP 方法区分（POST=点赞，DELETE=取消）
     */
    @DeleteMapping("/like")
    public RESTful<LikeVO> unlike(@Valid @RequestBody LikeDTO likeDTO) {
        log.info("用户:{}取消点赞:{}", UserContext.getUserId(), likeDTO.getBizId());
        LikeVO likeVO = likeInfoService.removeLikeInfo(likeDTO);
        return RESTful.success(likeVO, "已取消点赞");
    }

}

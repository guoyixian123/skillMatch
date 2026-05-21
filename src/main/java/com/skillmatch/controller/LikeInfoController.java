package com.skillmatch.controller;


import com.skillmatch.context.BeanContext;
import com.skillmatch.domain.dto.LikeDTO;
import com.skillmatch.domain.vo.LikeVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.ILikeInfoService;
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
    public RESTful<LikeVO> like(@RequestBody LikeDTO likeDTO) {
        log.info("用户:{}点赞了:{}", BeanContext.getUserId(), likeDTO.getBiz_id());
        LikeVO likeVO =likeInfoService.saveLikeInfo(likeDTO);
        return RESTful.success(likeVO);
    }

}

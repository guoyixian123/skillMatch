package com.skillmatch.controller;


import com.skillmatch.domain.query.PostCommentQuery;
import com.skillmatch.domain.query.PostQuery;
import com.skillmatch.domain.vo.CommentVO;
import com.skillmatch.domain.vo.PageVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.IPostCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Speed
 * @since 2026-05-23
 */
@Slf4j
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class PostCommentController {
    private final IPostCommentService postCommentService;
    /**
     * 获取帖子评论列表
     */
    @GetMapping("/posts/{postId}/comments")
    public RESTful<PageVO<CommentVO>> getPostComments(@PathVariable String postId, @ModelAttribute PostCommentQuery commentQuery) {
        log.info("获取帖子评论列表");
        PageVO<CommentVO> query = postCommentService.getComments(postId,commentQuery);
        return RESTful.success(query);
    }
    /**
     * 创建帖子评论
     */
    @PostMapping("/posts/{postId}/comments")
    public RESTful<Void> createPostComment(@PathVariable String postId, @RequestBody String body) {
        log.info("创建帖子评论");
        postCommentService.createPostComment(postId,body);
        return RESTful.success(null, "评论成功");
    }
    /**
     * 删除帖子评论
     */
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public RESTful<Void> deletePostComment(@PathVariable String postId, @PathVariable String commentId) {
        log.info("删除帖子id:{}评论", postId);
        postCommentService.removePostCommentById(postId,commentId);
        return RESTful.success(null, "评论已删除");
    }


}

package com.skillmatch.controller;


import com.skillmatch.domain.dto.PostDTO;
import com.skillmatch.domain.query.PostQuery;
import com.skillmatch.domain.vo.PageVO;
import com.skillmatch.domain.vo.PostVO;
import com.skillmatch.domain.vo.RESTful;
import com.skillmatch.service.IPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Speed
 * @since 2026-05-22
 */
@Slf4j
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class PostController {
    private final IPostService postService;
    /**
     * 获取帖子列表
     */
    @GetMapping("/posts")
    public RESTful<PageVO<PostVO>> getPost(@ModelAttribute PostQuery postDTO) {
        log.info("获取帖子列表");
       PageVO<PostVO> query = postService.queryPostList(postDTO);
        return RESTful.success(query);
    }

    /**
     * 创建帖子
     */
    @PostMapping("/posts")
    public RESTful<String> createPost(@Valid @RequestBody PostDTO postDTO) {
        log.info("创建帖子");
        postService.createPost(postDTO);
        return RESTful.success("发布帖子成功");
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/posts/{postId}")
    public RESTful<PostVO> getPost(@PathVariable String postId) {
        log.info("获取帖子详情");
        PostVO postVO = postService.getPostInfo(postId);
        return RESTful.success(postVO);
    }

    /**
     * 编辑帖子
     */
    @PutMapping("/posts/{postId}")
    public RESTful<String> updatePost(@PathVariable String postId, @RequestBody @Valid PostDTO postDTO) {
        log.info("更新帖子");
        postService.updatePost(postId, postDTO);
        return RESTful.success("更新帖子成功");
    }
    /**
     * 删除帖子
     */
    @DeleteMapping("/posts/{postId}")
    public RESTful<String> deletePost(@PathVariable String postId) {
        log.info("删除帖子");
        postService.removePostById(postId);
        return RESTful.success("删除帖子成功");
    }
}

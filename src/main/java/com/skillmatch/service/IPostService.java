package com.skillmatch.service;

import com.skillmatch.domain.dto.PostDTO;
import com.skillmatch.domain.po.Post;
import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.query.PostQuery;
import com.skillmatch.domain.vo.PageVO;
import com.skillmatch.domain.vo.PostVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Speed
 * @since 2026-05-22
 */
public interface IPostService extends IService<Post> {

    PageVO<PostVO> queryPostList(PostQuery postDTO);

    void createPost(PostDTO postDTO);

    PostVO getPostInfo(String postId);

    void updatePost(String postId, PostDTO postDTO);

    void removePostById(String postId);
}

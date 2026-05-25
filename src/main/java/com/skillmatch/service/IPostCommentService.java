package com.skillmatch.service;

import com.skillmatch.domain.po.PostComment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.query.PostCommentQuery;
import com.skillmatch.domain.vo.CommentVO;
import com.skillmatch.domain.vo.PageVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Speed
 * @since 2026-05-23
 */
public interface IPostCommentService extends IService<PostComment> {

    PageVO<CommentVO> getComments(String postId, PostCommentQuery commentQuery);

    void createPostComment(String postId, String body);

    void removePostCommentById(String postId, String commentId);
}

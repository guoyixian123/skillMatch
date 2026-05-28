package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.po.Post;
import com.skillmatch.domain.po.PostComment;
import com.skillmatch.domain.query.PostCommentQuery;
import com.skillmatch.domain.vo.CommentVO;
import com.skillmatch.domain.vo.PageVO;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.PostCommentMapper;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.service.IPostCommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.service.INotificationService;
import com.skillmatch.service.IPostService;
import com.skillmatch.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Speed
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class PostCommentServiceImpl extends ServiceImpl<PostCommentMapper, PostComment> implements IPostCommentService {
    private final UserMapper userMapper;
    private final IPostService postService;
    private final INotificationService notificationService;
    private final PostMapper postMapper;

    /**
     * 获取帖子评论列表
     */
    @Override
    public PageVO<CommentVO> getComments(String postId, PostCommentQuery commentQuery) {
        //获取帖子评论列表
        //1.查询帖子评论列表
        Page<PostComment> page = lambdaQuery()
                .eq(PostComment::getPostId, postId)
                .page(new Page<>(commentQuery.getPage(), commentQuery.getSize()));
        List<PostComment> records = page.getRecords();

        //获取评论用户id信息
        Set<String> userIds = new HashSet<>();
        records.forEach(record -> userIds.add(record.getUserId()));
        if (userIds.isEmpty()) {
            return new PageVO<CommentVO>(0L,  Integer.valueOf(0),  Integer.valueOf(0), Collections.emptyList());
        }
        Map<String, CommentVO.User> userMap = userMapper.selectByIds(userIds).stream()
                .map(user -> new CommentVO.User(user.getUserId(), user.getName(), user.getAvatarUrl()))
                .collect(Collectors.toMap(CommentVO.User::getId, user -> user));
        //封装评论信息VO
        List<CommentVO> commentVOS = new ArrayList<>(records.size());
        for (PostComment record : records) {
            CommentVO commentVO = BeanUtil.copyProperties(record, CommentVO.class);
            commentVO.setId(record.getId());
            //封装用户信息
            CommentVO.User user = userMap.get(record.getUserId());
            if (user != null) {
                commentVO.setUser(user);
            }
            commentVO.setBody(record.getBody());
            commentVO.setCreatedAt(record.getCreatedAt());
            commentVOS.add(commentVO);
        }
        //按照时间倒序
        commentVOS.sort(Comparator.comparing(CommentVO::getCreatedAt).reversed());
       // return new PageVO<>(page.getTotal(), page.getSize(), page.getCurrent(), commentVOS);
        return PageVO.of(page, commentVOS);
    }

    /**
     * 创建帖子评论
     */
    @Override
    @Transactional
    public void createPostComment(String postId, String body) {
        //创建帖子评论
        String userId = UserContext.getUserId();
        PostComment postComment = new PostComment();
        postComment.setUserId(userId);
        postComment.setPostId(postId);
        postComment.setBody(body);
        postComment.setCreatedAt(LocalDateTime.now());
        save(postComment);
        //帖子评论数+1
        postService.lambdaUpdate()
                .eq(Post::getId, postId)
                .setSql("comment_count = comment_count + 1")
                .update();
        //发送评论通知给帖子作者
        String authorId = postMapper.selectAuthorId(postId);
        if (authorId != null && !authorId.equals(userId)) {
            notificationService.save(userId, authorId, 3, postId);
        }
    }

    /**
     * 删除帖子评论
     */
    @Override
    @Transactional
    public void removePostCommentById(String postId, String commentId) {
        if(commentId == null|| commentId.isEmpty()|| postId == null|| postId.isEmpty()){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        //删除帖子评论
        removeById(commentId);
        //帖子评论数-1
        postService.lambdaUpdate()
                .eq(Post::getId, postId)
                .setSql("comment_count = comment_count - 1")
                .update();
    }
}

package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.PostDTO;
import com.skillmatch.domain.po.LikeInfo;
import com.skillmatch.domain.po.Post;
import com.skillmatch.domain.po.PostComment;
import com.skillmatch.domain.po.PostTag;
import com.skillmatch.domain.query.PostQuery;
import com.skillmatch.domain.vo.PageVO;
import com.skillmatch.domain.vo.PostVO;
import com.skillmatch.domain.vo.UserBasicVO;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.PostCommentMapper;
import com.skillmatch.mapper.PostMapper;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.service.ILikeInfoService;
import com.skillmatch.service.IPostService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.service.IPostTagService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Speed
 * @since 2026-05-22
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {
    private final UserMapper userMapper;
    private final IPostTagService postTagService;
    private final ILikeInfoService likeService;
    private final PostCommentMapper postCommentMapper;

    /**
     * 获取帖子列表
     */
    @Override
    public PageVO<PostVO> queryPostList(PostQuery postDTO) {
        //筛选帖子标签
        List<String> postIds = null;
        if (postDTO.getTag() != null && !postDTO.getTag().isBlank()) {
            List<PostTag> list = postTagService.lambdaQuery()
                    .eq(PostTag::getTagName, postDTO.getTag())
                    .list();
            //将帖子合格的id放入list
            postIds = list.stream().map(PostTag::getPostId).toList();
        }
        //获取帖子列表id
        Page<Post> page = lambdaQuery()
                .in(postIds != null, Post::getId, postIds)//批量查询
                .like(postDTO.getTag() != null, Post::getTitle, postDTO.getKeyword())//文字模糊查询
                .orderByAsc(postDTO.getSort().equals("latest"), Post::getCreatedAt)//最新排序
                .orderByAsc(postDTO.getSort().equals("host"), Post::getLikeCount)//最热排序(点赞数)
                .page(new Page<>(postDTO.getPage(), postDTO.getSize()));
        //获取帖子信息
        List<Post> records = page.getRecords();
        if (records.isEmpty()) {
            return null;
        }
        //开始封装vo
        //获取作者信息,头像,id,姓名
        String userId = UserContext.getUserId();
        List<PostVO> postVOList = new ArrayList<>(records.size());
        for (Post p : records) {
            PostVO postVO = getPostVO(p, userId);
            postVOList.add(postVO);
        }
        return new PageVO<>(page.getTotal(), page.getPages(), postVOList);
    }

    /**
     * 创建帖子
     */
    @Override
    @Transactional
    public void createPost(PostDTO postDTO) {
        //获取登录信息
        String userId = UserContext.getUserId();
        //每人每小时 5 篇上限，超限返回 429。
        Long count = lambdaQuery()
                .eq(Post::getAuthorId, userId)
                .ge(Post::getCreatedAt, LocalDateTime.now().minusHours(5))
                .count();
        if (count >= 5) {
            throw new RuntimeException("您已发布文章过多，请稍后再试");
        }
        //TODO:创建帖子后用户帖子数+1
        //创建帖子
        extracted(postDTO);
    }

    private @NonNull PostVO getPostVO(Post p, String userId) {
        PostVO postVO = new PostVO();
        BeanUtil.copyProperties(p, postVO);
        //获取作者信息
        UserBasicVO info = userMapper.getUserBasicInfo(p.getAuthorId());
        postVO.setAuthor(info);
        //获取标签信息
        List<PostTag> tags = postTagService.lambdaQuery()
                .eq(PostTag::getPostId, p.getId())
                .list();
        //批量获取标签名称
        List<String> tagNames = tags.stream().map(PostTag::getTagName).toList();
        postVO.setTags(tagNames);
        //获取点赞信息,判断是否点赞过
        Long count = likeService.lambdaQuery()
                .eq(LikeInfo::getUserId, userId)
                .eq(LikeInfo::getBizId, p.getId())
                .eq(LikeInfo::getType, 2)
                .count();
        postVO.setIsLiked(count > 0);
        //获取点赞数
        postVO.setLikeCount(p.getLikeCount());
        //获取评论数
        postVO.setCommentCount(p.getCommentCount());
        return postVO;
    }

    private void extracted(PostDTO postDTO) {
        //保存帖子信息
        String userId = UserContext.getUserId();
        //拷贝帖子信息
        Post post = BeanUtil.copyProperties(postDTO, Post.class);
        post.setAuthorId(userId);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        save(post);
        String id = post.getId();
        //保存标签信息
        List<String> tags = postDTO.getTags();
        //批量保存标签信息
        List<PostTag> postTags = new ArrayList<>();
        for (String tag : tags) {
            PostTag postTag = new PostTag();
            postTag.setPostId(id);
            postTag.setTagName(tag);
            postTags.add(postTag);
        }
        //批量保存帖子标签
        postTagService.saveBatch(postTags);
    }

    /**
     * 获取帖子详情
     */
    @Override
    public PostVO getPostInfo(String postId) {
        if(postId == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Post post = getById(postId);
        if(post == null){
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return getPostVO(post, UserContext.getUserId());
    }

    /**
     * 编辑帖子
     */
    @Override
    @Transactional
    public void updatePost(String postId, PostDTO postDTO) {
        if(postId == null|| postDTO == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        //先删除旧数据
        boolean remove = remove(lambdaQuery().eq(Post::getId, postId));
        if (!remove) {
            throw new RuntimeException("删除失败");
        }
        //保存新数据
        extracted(postDTO);
    }

    /**
     * 删除帖子
     */
    @Override
    @Transactional
    public void removePostById(String postId) {
        if(postId == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        //删除帖子要删除标签和点赞信息以及评论信息
        //删除帖子
        removeById(postId);
        //TODO:后期换成异步,删除帖子后用户帖子数-1
        //删除标签
        postTagService.lambdaUpdate()
                .eq(PostTag::getPostId, postId)
                .remove();
        //删除点赞信息
        likeService.lambdaUpdate()
                .eq(LikeInfo::getBizId, postId)
                .eq(LikeInfo::getType, 2)
                .remove();
        //删除评论信息
        postCommentMapper.delete(new QueryWrapper<PostComment>().eq("post_id", postId));
    }
}

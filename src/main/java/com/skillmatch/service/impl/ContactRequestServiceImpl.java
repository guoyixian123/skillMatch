package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.NotificationDTO;
import com.skillmatch.domain.po.ContactRequest;
import com.skillmatch.domain.po.User;
import com.skillmatch.domain.vo.NotificationVO;
import com.skillmatch.domain.vo.UserBasicVO;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.ContactRequestMapper;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.service.IContactRequestService;
import com.skillmatch.service.IFriendService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
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
 * @since 2026-05-25
 */
@Service
@RequiredArgsConstructor
public class ContactRequestServiceImpl extends ServiceImpl<ContactRequestMapper, ContactRequest> implements IContactRequestService {
    private final UserMapper userMapper;
    private final IFriendService friendService;

    /**
     * 获取收到的通知
     */
    @Override
    public List<NotificationVO> getNotification(Integer status) {
        String userId = UserContext.getUserId();

        //先判断是否有通知,取出通知信息
        List<ContactRequest> list = lambdaQuery()
                .eq(ContactRequest::getToUserId, userId)
                .eq(status != null, ContactRequest::getStatus, status)
                .list();
        if (list.isEmpty()) {
            //无通知,返回空
            return null;
        }
        //根据通知id查询发送通知的用户信息
        List<String> userIds = list.stream().map(ContactRequest::getFromUserId).toList();
        //根据通知id查询发送通知的用户信息
        //取出发送通知的用户id

        Set<UserBasicVO> userBasicInfos = new HashSet<>();
        for (String id : userIds) {
            UserBasicVO userBasicInfo = userMapper.getUserBasicInfo(id);
            userBasicInfos.add(userBasicInfo);
        }
        if (userBasicInfos.isEmpty()) {
            return null;
        }
        //用户信息,转换为map key:userid value:UserBasicVO(ID,name,url)
        Map<String, UserBasicVO> collect = userBasicInfos.stream().collect(Collectors.toMap(UserBasicVO::getId, e -> e));
        //封装vo
        List<NotificationVO> vos = new ArrayList<>(list.size());
        for (ContactRequest cr : list) {
            //创建vo
            NotificationVO vo = new NotificationVO();
            //拷贝属性到 vo
            BeanUtil.copyProperties(cr, vo);
            //封装用户信息
            vo.setFromUser(collect.get(cr.getFromUserId()));

            vos.add(vo);
        }
        return vos;
    }



    /**
     * 发送通知
     */
    @Override
    @Transactional
    public void sendNotification(NotificationDTO notificationDTO) {
        if (notificationDTO == null) {
            throw new RuntimeException("通知信息不能为空");
        }
        String from_user_id = UserContext.getUserId();

        //创建通知
        ContactRequest contactRequest = BeanUtil.copyProperties(notificationDTO, ContactRequest.class);
        contactRequest.setFromUserId(from_user_id);
        contactRequest.setCreatedAt(LocalDateTime.now());
        contactRequest.setUpdatedAt(LocalDateTime.now());
        //保存通知
        save(contactRequest);
    }

    /**
     * 接受通知
     */
    @Override
    @Transactional
    public String acceptRequest(String requestId) {
        String to_user_id = checkToUser(requestId);
        //获取请求信息
        ContactRequest request = getById(requestId);
        String from_user_id = request.getFromUserId();
        //获取用户信息
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserId, to_user_id));
        //更新通知状态为2
        lambdaUpdate()
                .eq(ContactRequest::getId, requestId)
                .set(ContactRequest::getStatus, 2)
                .set(ContactRequest::getUpdatedAt, LocalDateTime.now())
                .update();

        // 创建双向好友关系
        friendService.addFriend(from_user_id, to_user_id);

        //返回联系方式
        return user.getContactInfo();
    }

    /**
     * 检查用户id是否是接收用户,并且返回接收用户id
     */
    private String checkToUser(String requestId) {
        //参数校验,只有接收的用户可以接受
        String userId = UserContext.getUserId();
        if (requestId == null) {
            throw new RuntimeException("请求id不能为空");
        }
        //获取用户联系信息
        ContactRequest contactRequest = getById(requestId);
        if (contactRequest == null) {
            throw new RuntimeException("请求不存在");
        }
        //获取用户id
        String to_user_id = contactRequest.getToUserId();
        //判断是否是接收用户
        if (!Objects.equals(userId, to_user_id)) {
            throw new BusinessException(ErrorCode.NOT_AUTH, "没有权限");
        }
        return to_user_id;
    }

    /**
     * 拒绝通知
     */
    @Override
    public void declineRequest(String requestId) {
        //参数校验,只有接收的用户可以接受
        checkToUser(requestId);
        //更新状态为2:拒绝
        lambdaUpdate()
                .eq(ContactRequest::getId, requestId)
                .set(ContactRequest::getUpdatedAt, LocalDateTime.now())
                .set(ContactRequest::getStatus, 3)
                .update();
    }

    /**
     * 获取未处理通知数量
     */
    @Override
    public Integer getCountNotification() {
        //获取用户id
        String userId = UserContext.getUserId();
        //获取未处理通知数量
        return lambdaQuery()
                .eq(ContactRequest::getToUserId, userId)
                .eq(ContactRequest::getStatus, 1)
                .count().intValue();
    }

    /**
     * 获取已发送通知
     */
    @Override
    public List<NotificationVO> getSentNotification(Integer status) {
        //获取用户id
        String userId = UserContext.getUserId();
        //根据id判断该用户已发送通知
        List<ContactRequest> list = lambdaQuery()
                .eq(ContactRequest::getFromUserId, userId)
                .eq(status != null, ContactRequest::getStatus, status)
                .list();
        if (list.isEmpty()) {
            return null;
        }
        List<String> userIds = list.stream().map(ContactRequest::getToUserId).toList();
        //获取接收端的用户id
        //根据通知id查询发送通知的用户信息
        //取出发送通知的用户id

        Set<UserBasicVO> userBasicInfos = new HashSet<>();
        for (String id : userIds) {
            UserBasicVO userBasicInfo = userMapper.getUserBasicInfo(id);
            userBasicInfos.add(userBasicInfo);
        }
        if (userBasicInfos.isEmpty()) {
            return null;
        }
        //用户信息,转换为map key:userid value:UserBasicVO(ID,name,url)
        Map<String, UserBasicVO> collect = userBasicInfos.stream().collect(Collectors.toMap(UserBasicVO::getId, e -> e));
        //封装vo
        List<NotificationVO> vos = new ArrayList<>(list.size());
        for (ContactRequest cr : list) {
            //创建vo
            NotificationVO vo = new NotificationVO();
            //拷贝属性到 vo
            BeanUtil.copyProperties(cr, vo);
            //封装用户信息
            vo.setFromUser(collect.get(cr.getToUserId()));

            vos.add(vo);
        }
        return vos;
    }
}

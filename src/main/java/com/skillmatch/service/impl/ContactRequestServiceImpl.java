package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.skillmatch.ws.WebSocketSessionManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactRequestServiceImpl extends ServiceImpl<ContactRequestMapper, ContactRequest> implements IContactRequestService {
    private final UserMapper userMapper;
    private final IFriendService friendService;
    private final WebSocketSessionManager sessionManager;
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
            return List.of();
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
            return List.of();
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
            throw new BusinessException(ErrorCode.PARAM_ERROR, "通知信息不能为空");
        }
        String from_user_id = UserContext.getUserId();
        if (from_user_id.equals(notificationDTO.getToUserId())) {
            throw new BusinessException(ErrorCode.NOT_AUTH, "不能给自己发送技能交换请求");
        }
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

        // WebSocket 通知双方好友列表更新
        pushFriendUpdate(from_user_id, "new_friend");
        pushFriendUpdate(to_user_id, "new_friend");

        //返回联系方式
        return user.getContactInfo();
    }

    /**
     * 推送好友列表更新通知
     */
    private void pushFriendUpdate(String userId, String action) {
        WebSocketSession session = sessionManager.get(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> msg = new LinkedHashMap<>();
                msg.put("type", "friend_update");
                msg.put("action", action);
                session.sendMessage(new TextMessage(MAPPER.writeValueAsString(msg)));
                log.info("WS push friend_update: userId={}, action={}", userId, action);
            } catch (Exception e) {
                log.warn("WS push friend_update 失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查用户id是否是接收用户,并且返回接收用户id
     */
    private String checkToUser(String requestId) {
        //参数校验,只有接收的用户可以接受
        String userId = UserContext.getUserId();
        if (requestId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求id不能为空");
        }
        //获取用户联系信息
        ContactRequest contactRequest = getById(requestId);
        if (contactRequest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "请求不存在");
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
            return List.of();
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
            return List.of();
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

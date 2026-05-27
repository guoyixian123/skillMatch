package com.skillmatch.service;

import com.skillmatch.domain.dto.NotificationDTO;
import com.skillmatch.domain.po.ContactRequest;
import com.baomidou.mybatisplus.extension.service.IService;
import com.skillmatch.domain.vo.NotificationVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Speed
 * @since 2026-05-25
 */
public interface IContactRequestService extends IService<ContactRequest> {

    List<NotificationVO> getNotification(Integer status);

    void sendNotification(NotificationDTO notificationDTO);

    String acceptRequest(String requestId);

    void declineRequest(String requestId);

    Integer getCountNotification();

    List<NotificationVO> getSentNotification(Integer  status);
}

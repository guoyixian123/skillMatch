package com.skillmatch.service;

import com.skillmatch.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
public interface IUserService extends IService<User> {
    List<String> getProfile();
}

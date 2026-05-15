package com.skillmatch.service.impl;

import com.skillmatch.domain.po.User;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Speed
 * @since 2026-05-15
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}

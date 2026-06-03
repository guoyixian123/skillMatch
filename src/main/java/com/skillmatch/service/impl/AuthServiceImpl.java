package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.RegisterAndLoginDTO;
import com.skillmatch.domain.po.User;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.UserMapper;
import com.skillmatch.service.IAuthService;
import com.skillmatch.utils.JwtUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.skillmatch.constants.RedisConstant.LOGIN_TOKEN_KEY;
import static com.skillmatch.constants.FinalConstant.AUTH_EXPIRE;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements IAuthService {
    private final StringRedisTemplate redisTemplate;
    /*
    注册
     */
    @Override
    public Map<String, Object> register(RegisterAndLoginDTO register) {
        //注册信息不能为空
        if (register == null || register.getUserId() == null || register.getUserId().isBlank() || register.getPassword() == null || register.getPassword().isBlank())
        {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号和密码不能为空");
        }
        //判断账号是否重复
        Long count = lambdaQuery()
                .eq(User::getUserId, register.getUserId())
                .count();
                if(count > 0){
                    throw new BusinessException(ErrorCode.PARAM_ERROR,"用户已存在");
                }
        //md5加密处理
        String md5Password = SecureUtil.md5(register.getPassword());
        //重新修改用户传递的密码
        register.setPassword(md5Password);
        //拷贝注册信息到用户对象
        User user = BeanUtil.copyProperties(register, User.class);
        //设置创建时间
        user.setCreatedAt(LocalDateTime.now());
        //设置更新时间
        user.setUpdatedAt(LocalDateTime.now());
        //保存用户注册信息
        save(user);
        //封装返回信息
        return getStringObjectMap(user);
    }
    /*
    登录
     */
    @Override
    public Map<String, Object> login(RegisterAndLoginDTO login) {
        //登录信息不能为空
        if(login == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户信息不能为空");
        }
        //md5加密处理
        String md5Password = SecureUtil.md5(login.getPassword());
        //根据用户信息查询用户表
        User one = lambdaQuery()
                .eq(User::getUserId, login.getUserId())
                .eq(User::getPassword, md5Password)
                .one();
        //没有查找到数据
        if(one == null){
            throw  new BusinessException(ErrorCode.PARAM_ERROR,"用户名或密码错误");
        }
        // 检查账号是否被冻结
        if (one.getStatus() != null && one.getStatus() == 2) {
            throw new BusinessException(ErrorCode.NOT_AUTH, "账号已被冻结");
        }
        //设置最后登录时间
        lambdaUpdate()
                .eq(User::getUserId, login.getUserId())
                .eq(User::getPassword, md5Password)
                .set(User::getLastLoginAt, LocalDateTime.now())
                .update();
        //封装返回信息
        return getStringObjectMap(one);
    }

    /*
    封装返回信息
     */
    private  @NonNull Map<String, Object> getStringObjectMap(User user) {
        //生成TOKEN
        String token = JwtUtil.createToken(user.getUserId(), user.getName());
        //保存token到redis中,7天有效
        redisTemplate.opsForValue().set(LOGIN_TOKEN_KEY+user.getUserId(), token, AUTH_EXPIRE, TimeUnit.DAYS);
        //封装返回信息
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        Map<String, String> mapInfo = new HashMap<>();
        mapInfo.put("userId", user.getUserId());
        mapInfo.put("name", user.getName());
        mapInfo.put("avatarUrl", user.getAvatarUrl());
        map.put("user", mapInfo);
        return map;
    }
    /*
    刷新toekn
     */
    @Override
    public String refreshToken() {
        String userId = UserContext.getUserId();
        if(userId == null){
            return null;
        }
        log.info("用户id:{} 刷新token", userId);
        //获取redis中的token信息
        String token = redisTemplate.opsForValue().get(LOGIN_TOKEN_KEY+userId);
        if (token == null) {
            log.warn("用户id:{} 的token已过期", userId);
            return null;
        }
        Map<String, String> info = JwtUtil.parseToken(token);
        String id = info.get("userId");
        String name = info.get("name");
        //生成新的token
        String s1= JwtUtil.createToken(id, name);
        redisTemplate.opsForValue().set(LOGIN_TOKEN_KEY+userId, s1, AUTH_EXPIRE, TimeUnit.DAYS);
        return s1;
    }
    /*
    登出
     */
    @Override
    public void logout() {
        String userId = UserContext.getUserId();
        log.info("用户 id:{}退出登录", userId);
        //删除redis中的token,实现登出
        redisTemplate.delete(LOGIN_TOKEN_KEY+userId);
    }
}

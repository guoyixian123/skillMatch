package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.context.BeanContext;
import com.skillmatch.domain.dto.RegisterAndLogin;
import com.skillmatch.domain.po.User;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements IAuthService {
    private final StringRedisTemplate redisTemplate;
    /*
    注册
     */
    @Override
    public Map<String, Object> register(RegisterAndLogin register) {
        if(register == null){
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        //md5加密处理
        String md5Password = SecureUtil.md5(register.getPassword());
        //重新修改用户密码
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
    public Map<String, Object> login(RegisterAndLogin login) {
        //登录信息不能为空
        if(login == null){
            return null;
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
            return null;
        }
        return getStringObjectMap(one);
    }

    /*
    封装返回信息
     */
    private  @NonNull Map<String, Object> getStringObjectMap(User user) {
        //生成TOKEN
        String token = JwtUtil.createToken(user.getUserId(), user.getName());
        redisTemplate.opsForValue().set("login:token:"+user.getUserId(), token, 1, TimeUnit.DAYS);
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        Map<String, String> mapInfo = new HashMap<>();
        mapInfo.put("id", user.getUserId());
        mapInfo.put("name", user.getName());
        //TODO:封装头像信息
        mapInfo.put("avatarUrl", null);
        map.put("user", mapInfo);
        return map;
    }
    /*
    刷新toekn
     */
    @Override
    public String refreshToken() {
        String userId = BeanContext.getUserId();
        if(userId == null){
            return null;
        }
        log.info("用户id:{} 刷新token", userId);
        //获取redis中的token信息
        String token = redisTemplate.opsForValue().get("login:token:"+userId);
        Map<String, String> info = JwtUtil.parseToken(token);
        String id = info.get("userId");
        String name = info.get("name");
        //生成新的token
        String s1= JwtUtil.createToken(id, name);
        redisTemplate.opsForValue().set("login:token:"+userId, s1, 1, TimeUnit.DAYS);
        return s1;
    }
    /*
    登出
     */
    @Override
    public void logout() {
        String userId = BeanContext.getUserId();
        log.info("用户 id:{}退出登录", userId);
        //删除redis中的token,实现登出
        redisTemplate.delete("login:token:"+userId);
    }
}

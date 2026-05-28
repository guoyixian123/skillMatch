package com.skillmatch.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.po.ContactRequest;
import com.skillmatch.domain.po.LikeInfo;
import com.skillmatch.domain.po.User;
import com.skillmatch.domain.po.UserGallery;
import com.skillmatch.domain.po.UserHobby;
import com.skillmatch.domain.po.UserSkill;
import com.skillmatch.domain.query.MatchingQuery;
import com.skillmatch.domain.vo.*;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.service.IContactRequestService;
import com.skillmatch.service.ILikeInfoService;
import com.skillmatch.service.IMatchingService;
import com.skillmatch.service.IUserGalleryService;
import com.skillmatch.service.IUserHobbyService;
import com.skillmatch.service.IUserService;
import com.skillmatch.service.IUserSkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.skillmatch.constants.RedisConstant.USER_LOCATION_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingServiceImpl implements IMatchingService {
    private final IUserService userService;
    private final StringRedisTemplate redisTemplate;
    private final IUserSkillService userSkillService;
    private final IUserHobbyService userHobbyService;
    private final IContactRequestService contactRequestService;
    private final IUserGalleryService userGalleryService;
    private final ILikeInfoService likeInfoService;

    @Override
    public PageVO<UserCardVO> getRecommendedUsers(MatchingQuery query) {
        // 1. 获取当前用户位置，为空则返回空列表,获取数据库中的经纬度
        String userId = UserContext.getUserId();
        User one = userService.lambdaQuery()
                .eq(User::getUserId, userId)
                .one();
        if (one == null) {
            return new PageVO<>(0L, query.getPage(), query.getSize(), null);
        }
        //1.1获取当前用户经纬度
        double latitude = one.getLatitude();
        double longitude = one.getLongitude();
        //1.2根据经纬度查询附近用户
        Circle circle = new Circle(new Point(longitude, latitude), new Distance(query.getRadius(), Metrics.KILOMETERS));
        //1.3查询附近用户,以当前用户为圆心、radius 公里的圆, Redis GEO SEARCH 获取附近用户（COUNT 200）
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .radius(USER_LOCATION_KEY,
                        circle,
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeDistance()
                                .limit(200)
                );
        if (results == null) {
            return new PageVO<>(0L, query.getPage(), query.getSize(), null);
        }
        // 3. 过滤：排除本人 + 已有 pending 关系的用户
        Map<String, Double> userIdDistanceMap = new HashMap<>();//id-距离
        List<String> userIds = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results.getContent()) {
            //id
            String id = result.getContent().getName();
            if (id.equals(userId)) {
                continue;
            }
            //距离自己的距离
            double value = result.getDistance().getValue();
            //将用户信息添加进list
            userIds.add(id);
            //添加进map
            userIdDistanceMap.put(id, value);
        }
        if (userIdDistanceMap.isEmpty() || userIds.isEmpty()) {
            return new PageVO<>(0L, query.getPage(), query.getSize(), null);
        }

        // 4. 加载候选用户的技能标签和爱好
        //查询自己技能
        List<String> myCan = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillType, 1)
                .list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> myWant = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillType, 2)
                .list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> myHobbies = userHobbyService.lambdaQuery()
                .eq(UserHobby::getUserId, userId)
                .list()
                .stream().map(UserHobby::getHobbyName).toList();
        //查询其他用户技能
        List<UserSkill> iCanSkills = userSkillService.lambdaQuery()
                .in(UserSkill::getUserId, userIds)
                .eq(UserSkill::getSkillType, 1)
                .list();
        List<UserSkill> iWantSkills = userSkillService.lambdaQuery()
                .in(UserSkill::getUserId, userIds)
                .eq(UserSkill::getSkillType, 2)
                .list();
        List<UserHobby> iHobbies = userHobbyService.lambdaQuery()
                .in(UserHobby::getUserId, userIds)
                .list();
        if (iCanSkills == null || iWantSkills == null || iHobbies == null) {
            return new PageVO<>(0L, query.getPage(), query.getSize(), null);
        }
        // 5.2 按 userId 分组候选用户的技能和爱好
        Map<String, List<String>> canMap = new HashMap<>();
        for (UserSkill s : iCanSkills) {
            canMap.computeIfAbsent(s.getUserId(), k -> new ArrayList<>()).add(s.getSkillName());
        }
        Map<String, List<String>> wantMap = new HashMap<>();
        for (UserSkill s : iWantSkills) {
            wantMap.computeIfAbsent(s.getUserId(), k -> new ArrayList<>()).add(s.getSkillName());
        }
        Map<String, List<String>> hobbyMap = new HashMap<>();
        for (UserHobby h : iHobbies) {
            hobbyMap.computeIfAbsent(h.getUserId(), k -> new ArrayList<>()).add(h.getHobbyName());
        }
        // 5.3 查询候选用户的基本信息（name、avatarUrl、bio、likeCount）
        Map<String, User> userMap = userService.lambdaQuery()
                .in(User::getUserId, userIds)
                .list()
                .stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        // 5.5 关键词搜索过滤（昵称、技能名、爱好名模糊匹配）
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim().toLowerCase();
            userIds = userIds.stream()
                    .filter(uid -> {
                        User u = userMap.get(uid);
                        if (u == null) return false;
                        if (u.getName() != null && u.getName().toLowerCase().contains(kw)) return true;
                        List<String> can = canMap.getOrDefault(uid, List.of());
                        List<String> want = wantMap.getOrDefault(uid, List.of());
                        if (can.stream().anyMatch(s -> s.toLowerCase().contains(kw))) return true;
                        if (want.stream().anyMatch(s -> s.toLowerCase().contains(kw))) return true;
                        List<String> hobbies = hobbyMap.getOrDefault(uid, List.of());
                        return hobbies.stream().anyMatch(h -> h.toLowerCase().contains(kw));
                    })
                    .collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return new PageVO<>(0L, query.getPage(), query.getSize(), null);
            }
        }

        // 5.4 批量查询当前用户已点赞过的候选用户ID集合
        // 只查 type=1（个人主页点赞），用于批量设置卡片 isLiked 字段
        // 一次查询替代 N 次逐条查询，避免 N+1 问题
        Set<String> likedUserIds = likeInfoService.lambdaQuery()
                .eq(LikeInfo::getUserId, userId)
                .eq(LikeInfo::getType, 1)
                .in(LikeInfo::getBizId, userIds)
                .list()
                .stream().map(LikeInfo::getBizId)
                .collect(Collectors.toSet());

        // 5. 逐人计算互补度分数
        List<UserCardVO> cards = new ArrayList<>();
        for (String uid : userIds) {
            User user = userMap.get(uid);
            if (user == null) {
                log.warn("用户 {} 不存在，跳过", uid);
                continue;
            }
            List<String> theirCan = canMap.getOrDefault(uid, List.of());
            List<String> theirWant = wantMap.getOrDefault(uid, List.of());
            List<String> theirHobbies = hobbyMap.getOrDefault(uid, List.of());
            //我能教他们什么（我的"会" ∩ 他们的"想学"）,进行交集互补
            //TODO:后期可替换布隆过滤器
            //不满足条件的删掉
            //满足条件的留下
            long canTeach = theirWant.stream().filter(myCan::contains).count();//contains: 判断集合中是否包含某个元素,eg:list.contains("Java");   // true,list.contains("Python"); // false
            double canTeachRatio = theirWant.isEmpty() ? 0 : (double) canTeach / theirWant.size();

            long canLearn = theirCan.stream().filter(myWant::contains).count();
            double canLearnRatio = myWant.isEmpty() ? 0 : (double) canLearn / myWant.size();

            //互补度
            int matchScore =(int) Math.round((canTeachRatio + canLearnRatio) / 2 * 100);
            //共同的爱好,每个共享爱好 +5 分，上限 10 分
            long commonHobbies = theirHobbies.stream().filter(myHobbies::contains).count();
            int hobbyBonus = Math.min(10, (int) commonHobbies * 5);

            //匹配值
            matchScore = Math.min(100, (int) (matchScore * 0.9 + hobbyBonus));
            //构建卡片
            UserCardVO card = new UserCardVO();
            card.setUserId( uid);
            card.setName(userMap.get(uid).getName());
            card.setAvatarUrl(userMap.get(uid).getAvatarUrl());
            double distance = userIdDistanceMap.get(uid);
            card.setDistance(distance<1?Math.round(distance*1000)+"m":String.format("%.1fkm",distance));// 距离
            card.setCity(userMap.get(uid).getCity());
            card.setCanSkills(theirCan);
            card.setWantSkills(theirWant);
            card.setBio(userMap.get(uid).getBio());
            card.setMatchScore(matchScore);
            card.setLikeCount(userMap.get(uid).getLikeCount());
            // 根据批量查询的点赞集合设置 isLiked，避免逐条查库
            card.setIsLiked(likedUserIds.contains(uid));
            card.setAiScore(null);//ai未介入为null
            cards.add(card);
        }
        if(cards.isEmpty()){
            return new PageVO<>(0L, query.getPage(), query.getSize(), null);
        }
        // 6. 按 sort 参数排序（score/dist/active）
        switch (query.getSort()){
            case "score":{
                cards.sort(Comparator.comparing(UserCardVO::getMatchScore).reversed());
                break;
            }
            case "dist":{
                cards.sort(Comparator.comparing(card -> {
                    String dist = card.getDistance();
                    if (dist == null || dist.isEmpty()) {
                        return Double.MAX_VALUE;
                    }
                    try {
                        if (dist.endsWith("m")) {
                            return Double.parseDouble(dist.replace("m", ""));
                        } else if (dist.endsWith("km")) {
                            return Double.parseDouble(dist.replace("km", "")) * 1000;
                        }
                        return Double.parseDouble(dist);
                    } catch (NumberFormatException e) {
                        return Double.MAX_VALUE;
                    }
                }));
                break;
            }
            case "active":{
                cards.sort(Comparator.comparing(UserCardVO::getLikeCount).reversed());
                break;
            }
        }

        // 7. 若候选 >= 20，调用 AI 精排（try-catch 降级）
        // TODO:这里我这里先不考虑AI
        // 8. 分页返回
        int page = query.getPage() > 0 ? query.getPage() : 1;
        int size = query.getSize() > 0 ? query.getSize() : 10;
        long total = cards.size();
        int from = (page - 1) * size;//计算当前页的起始索引
        int to = Math.min(from + size, (int) total);//计算当前页的结束索引
        List<UserCardVO> pageList = from < total ? cards.subList(from, to) : List.of();
        return PageVO.of(new Page<UserCardVO>(page,size,total),pageList);
    }

    @Override
    public UserCardVO getUserCard(String userId) {
        // 1. 目标用户信息
        User target = userService.getById(userId);
        if (target == null) {
            return null;
        }

        // 2. 技能标签
        List<String> canSkills = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillType, 1)
                .list().stream().map(UserSkill::getSkillName).toList();
        List<String> wantSkills = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillType, 2)
                .list().stream().map(UserSkill::getSkillName).toList();

        // 3. 组装名片
        UserCardVO card = new UserCardVO();
        card.setUserId(target.getUserId());
        card.setName(target.getName());
        card.setAvatarUrl(target.getAvatarUrl());
        card.setSignature(target.getSignature());
        card.setCity(target.getCity());
        card.setCanSkills(canSkills);
        card.setWantSkills(wantSkills);
        card.setLikeCount(target.getLikeCount() != null ? target.getLikeCount() : 0);
        card.setPostCount(target.getPostCount() != null ? target.getPostCount() : 0);

        // 4. 已登录用户：计算距离、匹配度、是否已发请求
        String currentUserId = UserContext.getUserId();
        if (currentUserId != null && !currentUserId.equals(userId)) {
            // 距离
            User current = userService.getById(currentUserId);
            if (current != null && target.getLatitude() != 0 && target.getLongitude() != 0
                    && current.getLatitude() != 0 && current.getLongitude() != 0) {
                try {
                    double dist = redisTemplate.opsForGeo()
                            .distance(USER_LOCATION_KEY, currentUserId, userId, Metrics.KILOMETERS)
                            .getValue();
                    card.setDistance(dist < 1 ? Math.round(dist * 1000) + "m"
                            : String.format("%.1fkm", dist));
                } catch (Exception ignored) {
                    // GEO 中无此用户位置
                }
            }
            // 是否已发 pending 请求
            Long pending = contactRequestService.lambdaQuery()
                    .eq(ContactRequest::getFromUserId, currentUserId)
                    .eq(ContactRequest::getToUserId, userId)
                    .eq(ContactRequest::getStatus, 1)
                    .count();
            card.setHasPendingRequest(pending > 0);
            // 查询当前用户是否已赞过目标用户（type=1 个人主页点赞）
            Long liked = likeInfoService.lambdaQuery()
                    .eq(LikeInfo::getUserId, currentUserId)
                    .eq(LikeInfo::getBizId, userId)
                    .eq(LikeInfo::getType, 1)
                    .count();
            card.setIsLiked(liked > 0);
        }
        return card;
    }

    @Override
    public UserProfileVO getUserProfile(String userId) {
        // 1. 目标用户
        User target = userService.getById(userId);
        if (target == null) {
            return null;
        }

        // 2. 联系方式仅自己或双方已交换同意时可见
        String currentUserId = UserContext.getUserId();
        boolean isSelf = currentUserId != null && currentUserId.equals(userId);
        boolean canViewContact = isSelf;
        if (!isSelf && currentUserId != null) {
            Long accepted = contactRequestService.lambdaQuery()
                    .and(w -> w
                        .eq(ContactRequest::getFromUserId, currentUserId)
                        .eq(ContactRequest::getToUserId, userId)
                        .or()
                        .eq(ContactRequest::getFromUserId, userId)
                        .eq(ContactRequest::getToUserId, currentUserId))
                    .eq(ContactRequest::getStatus, 2)
                    .count();
            canViewContact = accepted > 0;
        }

        // 3. 技能
        List<String> canSkills = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillType, 1)
                .list().stream().map(UserSkill::getSkillName).toList();
        List<String> wantSkills = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillType, 2)
                .list().stream().map(UserSkill::getSkillName).toList();

        // 4. 爱好
        List<HobbyVO> hobbies = userHobbyService.lambdaQuery()
                .eq(UserHobby::getUserId, userId)
                .list().stream()
                .map(h -> {
                    HobbyVO vo = new HobbyVO();
                    vo.setHobbyName(h.getHobbyName());
                    vo.setIcon(h.getIcon());
                    return vo;
                }).toList();

        // 5. 相册
        List<String> gallery = userGalleryService.lambdaQuery()
                .eq(UserGallery::getUserId, userId)
                .orderByAsc(UserGallery::getSortOrder)
                .list().stream()
                .map(UserGallery::getImageUrl)
                .toList();

        // 6. 组装
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(target.getUserId());
        vo.setName(target.getName());
        vo.setAvatarUrl(target.getAvatarUrl());
        vo.setSignature(target.getSignature());
        vo.setBio(target.getBio());
        vo.setCity(target.getCity());
        vo.setContactInfo(target.getContactInfo());
        vo.setCanSkills(canSkills);
        vo.setWantSkills(wantSkills);
        vo.setHobbies(hobbies);
        vo.setGallery(gallery);
        vo.setLikeCount(target.getLikeCount() != null ? target.getLikeCount() : 0);
        vo.setPostCount(target.getPostCount() != null ? target.getPostCount() : 0);
        // 查询当前用户是否已赞过该用户主页（type=1）
        // 仅非本人访问时查询，自己看自己主页不需要 isLiked
        if (currentUserId != null && !isSelf) {
            Long liked = likeInfoService.lambdaQuery()
                    .eq(LikeInfo::getUserId, currentUserId)
                    .eq(LikeInfo::getBizId, userId)
                    .eq(LikeInfo::getType, 1)
                    .count();
            vo.setIsLiked(liked > 0);
        }
        vo.setActivities(List.of()); // user_activity 表待实现
        return vo;
    }
}

package com.skillmatch.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skillmatch.client.AIClient;
import com.skillmatch.config.GeoSyncRunner;
import com.skillmatch.domain.dto.AIProfile;
import com.skillmatch.domain.dto.AIMatchRequest;
import com.skillmatch.domain.dto.AIMatchResponse;
import com.skillmatch.domain.dto.MatchExplainRequest;
import com.skillmatch.domain.dto.MatchExplainResponse;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.po.*;
import com.skillmatch.domain.query.MatchingQuery;
import com.skillmatch.domain.vo.*;
import com.skillmatch.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.skillmatch.constants.FinalConstant.MATCHING_DISTANCE;
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
    private final GeoSyncRunner geoSyncRunner;
    private final IFriendService friendService;
    private final AIClient aiClient;

    @Override
    public PageVO<UserCardVO> getRecommendedUsers(MatchingQuery query) {
        // 1. 获取当前用户位置，为空则返回空列表,获取数据库中的经纬度
        String userId = UserContext.getUserId();
        User one = userService.lambdaQuery()
                .eq(User::getUserId, userId)
                .one();
        if (one == null) {
            return PageVO.of(0L, query.getPage(), query.getSize(), List.of());
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
        // 缓存未命中 → 从 MySQL 回源重建
        if (results == null || results.getContent().isEmpty()) {
            log.warn("Redis GEO 缓存未命中，从 MySQL 回源重建...");
            geoSyncRunner.syncAllUsersToGeo();  // 重建整个 GEO 缓存
            results = redisTemplate.opsForGeo().radius(USER_LOCATION_KEY,
                    circle,
                    RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                            .includeDistance()
                            .limit(200)
            );  // 重试一次
        }
        // 重建后仍为空 → 真的没有附近用户
        if (results == null || results.getContent().isEmpty()) {
            return PageVO.of(0L, query.getPage(), query.getSize(), List.of());
        }

        // 3. 过滤：排除本人 + 已有 pending 关系的用户
        log.info("匹配诊断: GEO 召回 {} 人", results.getContent().size());
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
        // ===== 批量查询需排除的用户（新增） =====
        Set<String> excludedUserIds = new HashSet<>();

        // ① 我发给别人的 pending 请求
        contactRequestService.lambdaQuery()
                .eq(ContactRequest::getFromUserId, userId)
                .eq(ContactRequest::getStatus, 1)
                .list()
                .forEach(r -> excludedUserIds.add(r.getToUserId()));

        // ② 别人发给我的 pending 请求
        contactRequestService.lambdaQuery()
                .eq(ContactRequest::getToUserId, userId)
                .eq(ContactRequest::getStatus, 1)
                .list()
                .forEach(r -> excludedUserIds.add(r.getFromUserId()));

        // ③ 已经是好友的（正向：我的好友列表）
        friendService.lambdaQuery()
                .eq(Friend::getUserId, userId)
                .list()
                .forEach(f -> excludedUserIds.add(f.getFriendId()));

        // ④ 已经是好友的（反向：别人把我加成好友的）
        friendService.lambdaQuery()
                .eq(Friend::getFriendId, userId)
                .list()
                .forEach(f -> excludedUserIds.add(f.getUserId()));

        log.info("匹配诊断: 排除自己后 {} 人, 待排除(pending+好友) {} 人", userIds.size(), excludedUserIds.size());

        // 从候选列表中一次性移除
        userIds.removeAll(excludedUserIds);
        excludedUserIds.forEach(userIdDistanceMap::remove);

        log.info("匹配诊断: 最终候选 {} 人", userIds.size());
        if (userIdDistanceMap.isEmpty() || userIds.isEmpty()) {
            return PageVO.of(0L, query.getPage(), query.getSize(), List.of());
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
            return PageVO.of(0L, query.getPage(), query.getSize(), List.of());
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
                return PageVO.of(0L, query.getPage(), query.getSize(), List.of());
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

        // 将List转为Set，contains从O(n)优化为O(1)
        Set<String> myCanSet = new HashSet<>(myCan);
        Set<String> myWantSet = new HashSet<>(myWant);
        Set<String> myHobbySet = new HashSet<>(myHobbies);

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
            long canTeach = theirWant.stream().filter(myCanSet::contains).count();
            double canTeachRatio = theirWant.isEmpty() ? 0 : (double) canTeach / theirWant.size();

            long canLearn = theirCan.stream().filter(myWantSet::contains).count();
            double canLearnRatio = myWantSet.isEmpty() ? 0 : (double) canLearn / myWantSet.size();

            //技能互补度 (0-100)
            int skillComplement = (int) Math.round((canTeachRatio + canLearnRatio) / 2 * 100);
            //兴趣重叠度 (0-100)，Jaccard 相似系数
            long commonHobbies = theirHobbies.stream().filter(myHobbySet::contains).count();
            int hobbyOverlap = 0;
            int unionSize = myHobbySet.size() + theirHobbies.size() - (int) commonHobbies;
            if (unionSize > 0) {
                hobbyOverlap = (int) Math.round((double) commonHobbies / unionSize * 100);
            }
            //匹配值：技能互补 70% + 兴趣重叠 30%
            int matchScore = Math.min(100, (int) (skillComplement * 0.7 + hobbyOverlap * 0.3));
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
            return PageVO.of(0L, query.getPage(), query.getSize(), List.of());
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
        // 触发条件：候选人数 >= 20，避免对小批量做无效的 HTTP 调用
        // 策略：SentenceTransformer 语义相似度(30%) + 技能互补度(40%) + 兴趣重叠度(30%)
        //       与原有规则匹配分(60%) 加权融合 → 重排序
        // 降级：AI 服务不可用/超时/返回空 → 静默降级，继续使用规则排序结果
        if (cards.size() >= 20) {
            try {
                // 7.1 构建当前用户的语义画像（技能 + 兴趣 + 简介文本）
                AIMatchRequest aiReq = new AIMatchRequest();
                AIProfile sp = new AIProfile();
                sp.setUserId(userId);
                sp.setCanSkills(myCan);
                sp.setWantSkills(myWant);
                sp.setHobbies(myHobbies);
                aiReq.setSource(sp);

                List<AIProfile> cps = new ArrayList<>();
                for (UserCardVO c : cards) {
                    AIProfile p = new AIProfile();
                    p.setUserId(c.getUserId());
                    p.setCanSkills(canMap.getOrDefault(c.getUserId(), List.of()));
                    p.setWantSkills(wantMap.getOrDefault(c.getUserId(), List.of()));
                    p.setHobbies(hobbyMap.getOrDefault(c.getUserId(), List.of()));
                    cps.add(p);
                }
                aiReq.setCandidates(cps);

                // 异步调用AI引擎，不阻塞Servlet线程
                AIMatchResponse aiResp = aiClient.batchMatchAsync(aiReq).join();
                if (aiResp != null && aiResp.getScores() != null) {
                    Map<String, Double> aiMap = new HashMap<>();
                    for (AIMatchResponse.MatchScore s : aiResp.getScores()) {
                        aiMap.put(s.getUserId(), s.getScore());
                    }
                    // 7.5 加权融合：原有规则匹配分 × 60% + AI 语义分 × 40%（归一化到 0-100）
                    for (UserCardVO c : cards) {
                        double ai = aiMap.getOrDefault(c.getUserId(), 0.0);
                        c.setMatchScore((int)(c.getMatchScore() * 0.6 + ai * 40));
                    }
                    // 7.6 仅当用户选择按匹配度排序时才重排，其他排序方式保持用户选择
                    if ("score".equals(query.getSort())) {
                        cards.sort(Comparator.comparing(UserCardVO::getMatchScore).reversed());
                    }
                }
            } catch (Exception e) {
                // AI 服务不可用时静默降级，不影响用户体验
                log.warn("AI 精排失败，降级使用规则排序: {}", e.getMessage());
            }
        }

        //分页返回
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
    public String getMatchReason(String targetUserId) {
        String currentUserId = UserContext.getUserId();
        if (currentUserId == null || currentUserId.equals(targetUserId)) {
            return null;
        }

        User current = userService.getById(currentUserId);
        User target = userService.getById(targetUserId);
        if (current == null || target == null) {
            return null;
        }

        try {
            List<String> myCan = userSkillService.lambdaQuery()
                    .eq(UserSkill::getUserId, currentUserId)
                    .eq(UserSkill::getSkillType, 1)
                    .list().stream().map(UserSkill::getSkillName).toList();
            List<String> myWant = userSkillService.lambdaQuery()
                    .eq(UserSkill::getUserId, currentUserId)
                    .eq(UserSkill::getSkillType, 2)
                    .list().stream().map(UserSkill::getSkillName).toList();
            List<String> myHobbies = userHobbyService.lambdaQuery()
                    .eq(UserHobby::getUserId, currentUserId)
                    .list().stream().map(UserHobby::getHobbyName).toList();

            List<String> targetCan = userSkillService.lambdaQuery()
                    .eq(UserSkill::getUserId, targetUserId)
                    .eq(UserSkill::getSkillType, 1)
                    .list().stream().map(UserSkill::getSkillName).toList();
            List<String> targetWant = userSkillService.lambdaQuery()
                    .eq(UserSkill::getUserId, targetUserId)
                    .eq(UserSkill::getSkillType, 2)
                    .list().stream().map(UserSkill::getSkillName).toList();
            List<String> targetHobbies = userHobbyService.lambdaQuery()
                    .eq(UserHobby::getUserId, targetUserId)
                    .list().stream().map(UserHobby::getHobbyName).toList();

            MatchExplainRequest explainReq = new MatchExplainRequest();
            explainReq.setSourceName(current.getName());
            explainReq.setSourceBio(current.getBio());
            explainReq.setSourceCanSkills(myCan);
            explainReq.setSourceWantSkills(myWant);
            explainReq.setSourceHobbies(myHobbies);
            explainReq.setTargetName(target.getName());
            explainReq.setTargetBio(target.getBio());
            explainReq.setTargetCanSkills(targetCan);
            explainReq.setTargetWantSkills(targetWant);
            explainReq.setTargetHobbies(targetHobbies);

            log.info("LLM explain: {} ↔ {}", current.getName(), target.getName());
            MatchExplainResponse resp = aiClient.explainMatch(explainReq);
            if (resp != null && resp.getReason() != null) {
                log.info("LLM explain 成功: {}", resp.getReason());
                return resp.getReason();
            }
            log.info("LLM explain 返回空");
        } catch (Exception e) {
            log.warn("LLM explain 异常: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public PageVO<UserCardVO> searchUsers(String keyword, int page, int size) {
        String currentUserId = UserContext.getUserId();

        if (keyword == null || keyword.isBlank()) {
            return PageVO.of(0L, page, size, List.of());
        }
        String kw = keyword.trim();

        // 1. 从技能表和爱好表找匹配的用户ID
        Set<String> skillUserIds = userSkillService.lambdaQuery()
                .like(UserSkill::getSkillName, kw).list()
                .stream().map(UserSkill::getUserId).collect(Collectors.toSet());
        Set<String> hobbyUserIds = userHobbyService.lambdaQuery()
                .like(UserHobby::getHobbyName, kw).list()
                .stream().map(UserHobby::getUserId).collect(Collectors.toSet());
        Set<String> extraUserIds = new HashSet<>();
        extraUserIds.addAll(skillUserIds);
        extraUserIds.addAll(hobbyUserIds);

        // 2. SQL层过滤：name/bio 匹配 或 userId 在技能/爱好匹配中
        Page<User> userPage = userService.lambdaQuery()
                .ne(User::getUserId, currentUserId)
                .eq(User::getStatus, 1)
                .and(w -> w
                        .like(User::getName, kw)
                        .or()
                        .like(User::getBio, kw)
                        .or()
                        .in(!extraUserIds.isEmpty(), User::getUserId, extraUserIds)
                )
                .orderByDesc(User::getLastLoginAt)
                .page(new Page<>(page, size));

        List<User> users = userPage.getRecords();
        if (users.isEmpty()) {
            return PageVO.of(0L, page, size, List.of());
        }

        List<String> userIds = users.stream().map(User::getUserId).toList();

        // 3. 批量查技能
        Map<String, List<String>> canMap = userSkillService.lambdaQuery()
                .in(UserSkill::getUserId, userIds).eq(UserSkill::getSkillType, 1).list()
                .stream().collect(Collectors.groupingBy(UserSkill::getUserId, Collectors.mapping(UserSkill::getSkillName, Collectors.toList())));
        Map<String, List<String>> wantMap = userSkillService.lambdaQuery()
                .in(UserSkill::getUserId, userIds).eq(UserSkill::getSkillType, 2).list()
                .stream().collect(Collectors.groupingBy(UserSkill::getUserId, Collectors.mapping(UserSkill::getSkillName, Collectors.toList())));

        // 4. 构建结果
        List<UserCardVO> results = users.stream()
                .map(u -> {
                    UserCardVO card = new UserCardVO();
                    card.setUserId(u.getUserId());
                    card.setName(u.getName());
                    card.setAvatarUrl(u.getAvatarUrl());
                    card.setCity(u.getCity());
                    card.setBio(u.getBio());
                    card.setCanSkills(canMap.getOrDefault(u.getUserId(), List.of()));
                    card.setWantSkills(wantMap.getOrDefault(u.getUserId(), List.of()));
                    card.setLikeCount(u.getLikeCount() != null ? u.getLikeCount() : 0);
                    return card;
                })
                .toList();

        return PageVO.of(userPage.getTotal(), page, size, results);
    }

    @Override
    public List<UserCardVO> getDiscoverUsers(String matchType, List<String> excludeUserIds) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return List.of();
        }

        User currentUser = userService.lambdaQuery()
                .eq(User::getUserId, userId)
                .one();
        if (currentUser == null) {
            return List.of();
        }

        boolean isNearby = "nearby".equals(matchType);
        Map<String, Double> userIdDistanceMap = new HashMap<>();
        List<String> candidateIds = new ArrayList<>();

        if (isNearby) {
            // 附近匹配：使用 Redis GEO 召回 100km 内用户
            double latitude = currentUser.getLatitude();
            double longitude = currentUser.getLongitude();
            if (latitude == 0 && longitude == 0) {
                return List.of();
            }

            Circle circle = new Circle(new Point(longitude, latitude), new Distance(100, Metrics.KILOMETERS));
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                    .radius(USER_LOCATION_KEY, circle,
                            RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                    .includeDistance()
                                    .limit(200));

            if (results == null || results.getContent().isEmpty()) {
                geoSyncRunner.syncAllUsersToGeo();
                results = redisTemplate.opsForGeo().radius(USER_LOCATION_KEY, circle,
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeDistance()
                                .limit(200));
            }

            if (results == null || results.getContent().isEmpty()) {
                return List.of();
            }

            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results.getContent()) {
                String id = result.getContent().getName();
                if (id.equals(userId)) continue;
                candidateIds.add(id);
                userIdDistanceMap.put(id, result.getDistance().getValue());
            }
        } else {
            // 普通匹配：从全库随机获取活跃用户（不限距离）
            List<User> allUsers = userService.lambdaQuery()
                    .ne(User::getUserId, userId)
                    .eq(User::getStatus, 1)
                    .orderByDesc(User::getLastLoginAt)
                    .last("LIMIT 200")
                    .list();
            for (User u : allUsers) {
                candidateIds.add(u.getUserId());
            }
        }

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        // 排除：已发pending请求、已是好友
        Set<String> excludedSet = new HashSet<>();
        contactRequestService.lambdaQuery()
                .eq(ContactRequest::getFromUserId, userId).eq(ContactRequest::getStatus, 1).list()
                .forEach(r -> excludedSet.add(r.getToUserId()));
        contactRequestService.lambdaQuery()
                .eq(ContactRequest::getToUserId, userId).eq(ContactRequest::getStatus, 1).list()
                .forEach(r -> excludedSet.add(r.getFromUserId()));
        friendService.lambdaQuery().eq(Friend::getUserId, userId).list()
                .forEach(f -> excludedSet.add(f.getFriendId()));
        friendService.lambdaQuery().eq(Friend::getFriendId, userId).list()
                .forEach(f -> excludedSet.add(f.getUserId()));

        // 排除前端传入的已展示用户
        if (excludeUserIds != null) {
            excludedSet.addAll(excludeUserIds);
        }

        candidateIds.removeAll(excludedSet);
        userIdDistanceMap.keySet().removeAll(excludedSet);

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        // 加载当前用户技能和爱好
        List<String> myCan = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId).eq(UserSkill::getSkillType, 1).list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> myWant = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId).eq(UserSkill::getSkillType, 2).list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> myHobbies = userHobbyService.lambdaQuery()
                .eq(UserHobby::getUserId, userId).list()
                .stream().map(UserHobby::getHobbyName).toList();

        // 批量加载候选用户技能和爱好
        List<UserSkill> candidateCanSkills = userSkillService.lambdaQuery()
                .in(UserSkill::getUserId, candidateIds).eq(UserSkill::getSkillType, 1).list();
        List<UserSkill> candidateWantSkills = userSkillService.lambdaQuery()
                .in(UserSkill::getUserId, candidateIds).eq(UserSkill::getSkillType, 2).list();
        List<UserHobby> candidateHobbies = userHobbyService.lambdaQuery()
                .in(UserHobby::getUserId, candidateIds).list();

        Map<String, List<String>> canMap = new HashMap<>();
        for (UserSkill s : candidateCanSkills) {
            canMap.computeIfAbsent(s.getUserId(), k -> new ArrayList<>()).add(s.getSkillName());
        }
        Map<String, List<String>> wantMap = new HashMap<>();
        for (UserSkill s : candidateWantSkills) {
            wantMap.computeIfAbsent(s.getUserId(), k -> new ArrayList<>()).add(s.getSkillName());
        }
        Map<String, List<String>> hobbyMap = new HashMap<>();
        for (UserHobby h : candidateHobbies) {
            hobbyMap.computeIfAbsent(h.getUserId(), k -> new ArrayList<>()).add(h.getHobbyName());
        }

        // 批量查询候选用户基本信息
        Map<String, User> userMap = userService.lambdaQuery()
                .in(User::getUserId, candidateIds).list()
                .stream().collect(Collectors.toMap(User::getUserId, u -> u));

        Set<String> myCanSet = new HashSet<>(myCan);
        Set<String> myWantSet = new HashSet<>(myWant);

        // 计算匹配分数用于排序
        List<Map.Entry<String, Double>> scoredCandidates = new ArrayList<>();
        for (String uid : candidateIds) {
            if (!userMap.containsKey(uid)) continue;
            List<String> theirCan = canMap.getOrDefault(uid, List.of());
            List<String> theirWant = wantMap.getOrDefault(uid, List.of());

            long canTeach = theirWant.stream().filter(myCanSet::contains).count();
            double canTeachRatio = theirWant.isEmpty() ? 0 : (double) canTeach / theirWant.size();
            long canLearn = theirCan.stream().filter(myWantSet::contains).count();
            double canLearnRatio = myWantSet.isEmpty() ? 0 : (double) canLearn / myWantSet.size();
            int skillComplement = (int) Math.round((canTeachRatio + canLearnRatio) / 2 * 100);

            List<String> theirHobbies = hobbyMap.getOrDefault(uid, List.of());
            long commonHobbies = theirHobbies.stream().filter(myHobbies::contains).count();
            int hobbyOverlap = 0;
            int unionSize = myHobbies.size() + theirHobbies.size() - (int) commonHobbies;
            if (unionSize > 0) {
                hobbyOverlap = (int) Math.round((double) commonHobbies / unionSize * 100);
            }
            int matchScore = Math.min(100, (int) (skillComplement * 0.7 + hobbyOverlap * 0.3));

            scoredCandidates.add(Map.entry(uid, (double) matchScore));
        }

        // 按匹配分数降序排序
        scoredCandidates.sort(Comparator.<Map.Entry<String, Double>, Double>comparing(Map.Entry::getValue).reversed());

        // 从前30个候选中随机选取6个
        List<UserCardVO> result = new ArrayList<>();
        List<String> selectedIds = new ArrayList<>();
        Random random = new Random();
        int poolSize = Math.min(scoredCandidates.size(), 30);
        List<String> pool = scoredCandidates.subList(0, poolSize).stream()
                .map(Map.Entry::getKey).collect(Collectors.toList());

        while (result.size() < 6 && !pool.isEmpty()) {
            int idx = random.nextInt(pool.size());
            String selectedId = pool.remove(idx);
            if (selectedIds.contains(selectedId)) continue;
            selectedIds.add(selectedId);

            User user = userMap.get(selectedId);
            if (user == null) continue;

            List<String> theirCan = canMap.getOrDefault(selectedId, List.of());
            List<String> theirWant = wantMap.getOrDefault(selectedId, List.of());
            List<String> theirHobbies = hobbyMap.getOrDefault(selectedId, List.of());

            // 先用规则生成降级建议
            String fallbackSuggestion = generateAiSuggestion(myCan, myWant, myHobbies, theirCan, theirWant, theirHobbies, user.getName());

            UserCardVO card = new UserCardVO();
            card.setUserId(selectedId);
            card.setName(user.getName());
            card.setAvatarUrl(user.getAvatarUrl());
            // 设置距离
            if (isNearby) {
                double distance = userIdDistanceMap.getOrDefault(selectedId, 0.0);
                card.setDistance(distance < 1 ? Math.round(distance * 1000) + "m" : String.format("%.1fkm", distance));
            } else {
                try {
                    Distance dist = redisTemplate.opsForGeo()
                        .distance(USER_LOCATION_KEY, userId, selectedId, Metrics.KILOMETERS);
                    if (dist != null) {
                        double value = dist.getValue();
                        if (value > 0) {
                            card.setDistance(value < 1 ? Math.round(value * 1000) + "m" : String.format("%.1fkm", value));
                        }
                    }
                } catch (Exception ignored) {}
            }
            card.setCity(user.getCity());
            card.setCanSkills(theirCan);
            card.setWantSkills(theirWant);
            card.setBio(user.getBio());
            card.setLikeCount(user.getLikeCount() != null ? user.getLikeCount() : 0);
            card.setAiSuggestion(fallbackSuggestion);
            result.add(card);
        }

        return result;
    }

    @Override
    public int getMatchScore(String targetUserId) {
        String userId = UserContext.getUserId();
        if (userId == null || userId.equals(targetUserId)) {
            return 0;
        }

        List<String> myCan = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId).eq(UserSkill::getSkillType, 1).list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> myWant = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId).eq(UserSkill::getSkillType, 2).list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> myHobbies = userHobbyService.lambdaQuery()
                .eq(UserHobby::getUserId, userId).list()
                .stream().map(UserHobby::getHobbyName).toList();

        List<String> targetCan = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, targetUserId).eq(UserSkill::getSkillType, 1).list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> targetWant = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, targetUserId).eq(UserSkill::getSkillType, 2).list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> targetHobbies = userHobbyService.lambdaQuery()
                .eq(UserHobby::getUserId, targetUserId).list()
                .stream().map(UserHobby::getHobbyName).toList();

        // 1. 规则计算
        Set<String> myCanSet = new HashSet<>(myCan);
        Set<String> myWantSet = new HashSet<>(myWant);

        long canTeach = targetWant.stream().filter(myCanSet::contains).count();
        double canTeachRatio = targetWant.isEmpty() ? 0 : (double) canTeach / targetWant.size();
        long canLearn = targetCan.stream().filter(myWantSet::contains).count();
        double canLearnRatio = myWantSet.isEmpty() ? 0 : (double) canLearn / myWantSet.size();
        int skillComplement = (int) Math.round((canTeachRatio + canLearnRatio) / 2 * 100);

        long commonHobbies = targetHobbies.stream().filter(myHobbies::contains).count();
        int hobbyOverlap = 0;
        int unionSize = myHobbies.size() + targetHobbies.size() - (int) commonHobbies;
        if (unionSize > 0) {
            hobbyOverlap = (int) Math.round((double) commonHobbies / unionSize * 100);
        }
        int ruleScore = Math.min(100, (int) (skillComplement * 0.7 + hobbyOverlap * 0.3));

        // 2. AI 语义计算（降级：AI 不可用时只用规则分）
        try {
            AIMatchRequest aiReq = new AIMatchRequest();
            AIProfile source = new AIProfile();
            source.setUserId(userId);
            source.setCanSkills(myCan);
            source.setWantSkills(myWant);
            source.setHobbies(myHobbies);
            aiReq.setSource(source);

            AIProfile target = new AIProfile();
            target.setUserId(targetUserId);
            target.setCanSkills(targetCan);
            target.setWantSkills(targetWant);
            target.setHobbies(targetHobbies);
            aiReq.setCandidates(List.of(target));

            AIMatchResponse aiResp = aiClient.batchMatchAsync(aiReq).join();
            if (aiResp != null && aiResp.getScores() != null && !aiResp.getScores().isEmpty()) {
                double aiScore = aiResp.getScores().get(0).getScore();
                // 加权融合：规则 60% + AI 40%
                return Math.min(100, (int) (ruleScore * 0.6 + aiScore * 40));
            }
        } catch (Exception e) {
            log.warn("AI 匹配分计算失败，降级使用规则分: {}", e.getMessage());
        }

        return ruleScore;
    }

    /**
     * 根据双方技能和爱好生成 AI 建议
     */
    private String generateAiSuggestion(List<String> myCan, List<String> myWant, List<String> myHobbies,
                                         List<String> theirCan, List<String> theirWant, List<String> theirHobbies,
                                         String targetName) {
        // 找出互补技能
        List<String> iCanTeachThem = myCan.stream().filter(theirWant::contains).toList();
        List<String> theyCanTeachMe = theirCan.stream().filter(myWant::contains).toList();
        List<String> commonHobbiesList = myHobbies.stream().filter(theirHobbies::contains).toList();

        StringBuilder suggestion = new StringBuilder();

        if (!iCanTeachThem.isEmpty() && !theyCanTeachMe.isEmpty()) {
            suggestion.append(String.format("你们在 %s 和 %s 学习上有很好的互补性",
                    String.join("、", iCanTeachThem.subList(0, Math.min(2, iCanTeachThem.size()))),
                    String.join("、", theyCanTeachMe.subList(0, Math.min(2, theyCanTeachMe.size())))));
            if (!commonHobbiesList.isEmpty()) {
                suggestion.append(String.format("，而且都喜欢 %s", String.join("和", commonHobbiesList.subList(0, Math.min(2, commonHobbiesList.size())))));
            }
            suggestion.append("。");
        } else if (!iCanTeachThem.isEmpty()) {
            suggestion.append(String.format("你可以教 %s %s，这是很好的技能交换机会。",
                    targetName, String.join("和", iCanTeachThem.subList(0, Math.min(2, iCanTeachThem.size())))));
        } else if (!theyCanTeachMe.isEmpty()) {
            suggestion.append(String.format("%s 可以教你 %s，非常值得交流学习。",
                    targetName, String.join("和", theyCanTeachMe.subList(0, Math.min(2, theyCanTeachMe.size())))));
        } else if (!commonHobbiesList.isEmpty()) {
            suggestion.append(String.format("你们都对 %s 有浓厚兴趣，可以一起探讨交流。",
                    String.join("和", commonHobbiesList.subList(0, Math.min(2, commonHobbiesList.size())))));
        } else {
            suggestion.append(String.format("和 %s 交流一下，也许能发现新的学习方向。", targetName));
        }

        return suggestion.toString();
    }

    @Override
    public String getAiSuggestion(String targetUserId) {
        String userId = UserContext.getUserId();
        if (userId == null || userId.equals(targetUserId)) {
            return null;
        }

        User currentUser = userService.lambdaQuery().eq(User::getUserId, userId).one();
        User target = userService.lambdaQuery().eq(User::getUserId, targetUserId).one();
        if (currentUser == null || target == null) {
            return null;
        }

        List<String> myCan = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId).eq(UserSkill::getSkillType, 1).list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> myWant = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, userId).eq(UserSkill::getSkillType, 2).list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> myHobbies = userHobbyService.lambdaQuery()
                .eq(UserHobby::getUserId, userId).list()
                .stream().map(UserHobby::getHobbyName).toList();

        List<String> targetCan = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, targetUserId).eq(UserSkill::getSkillType, 1).list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> targetWant = userSkillService.lambdaQuery()
                .eq(UserSkill::getUserId, targetUserId).eq(UserSkill::getSkillType, 2).list()
                .stream().map(UserSkill::getSkillName).toList();
        List<String> targetHobbies = userHobbyService.lambdaQuery()
                .eq(UserHobby::getUserId, targetUserId).list()
                .stream().map(UserHobby::getHobbyName).toList();

        try {
            MatchExplainRequest explainReq = new MatchExplainRequest();
            explainReq.setSourceName(currentUser.getName());
            explainReq.setSourceBio(currentUser.getBio() != null ? currentUser.getBio() : "");
            explainReq.setSourceCanSkills(myCan);
            explainReq.setSourceWantSkills(myWant);
            explainReq.setSourceHobbies(myHobbies);
            explainReq.setTargetName(target.getName());
            explainReq.setTargetBio(target.getBio() != null ? target.getBio() : "");
            explainReq.setTargetCanSkills(targetCan);
            explainReq.setTargetWantSkills(targetWant);
            explainReq.setTargetHobbies(targetHobbies);
            explainReq.setLlmTimeout(5);

            MatchExplainResponse resp = aiClient.explainMatch(explainReq);
            if (resp != null && resp.getReason() != null && !resp.getReason().isBlank()) {
                log.info("AI 建议成功: {} ↔ {}", currentUser.getName(), target.getName());
                return resp.getReason();
            }
        } catch (Exception e) {
            log.warn("AI 建议失败: {}", e.getMessage());
        }
        return null;
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

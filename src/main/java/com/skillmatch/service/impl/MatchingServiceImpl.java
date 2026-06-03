package com.skillmatch.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skillmatch.config.GeoSyncRunner;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.po.*;
import com.skillmatch.domain.query.MatchingQuery;
import com.skillmatch.domain.vo.*;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.service.*;
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
    private final GeoSyncRunner geoSyncRunner;
    private final IFriendService friendService;

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

        // 从候选列表中一次性移除
        userIds.removeAll(excludedUserIds);
        excludedUserIds.forEach(userIdDistanceMap::remove);

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

        // 7. 若候选 >= 20，调用 AI 精排（try-catch 降级)
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

    @Override
    public List<CityDistributionVO> getCityDistribution() {
        // 查询所有有城市信息的用户，按城市分组统计总人数（不区分真人和机器人）
        List<User> all = userService.lambdaQuery()
                .isNotNull(User::getCity)
                .ne(User::getCity, "")
                .ne(User::getCity, "未知")
                .list();
        // 按城市名分组计数
        Map<String, Integer> cityCount = new LinkedHashMap<>();
        for (User u : all) {
            cityCount.merge(u.getCity(), 1, Integer::sum);
        }
        // 转为 VO 并按人数降序排列
        return cityCount.entrySet().stream()
                .map(e -> new CityDistributionVO(e.getKey(), e.getValue()))
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProvinceDistributionVO> getProvinceDistribution() {
        // 查询所有有城市信息的用户
        List<User> all = userService.lambdaQuery()
                .isNotNull(User::getCity)
                .ne(User::getCity, "")
                .ne(User::getCity, "未知")
                .list();

        // 按省份分组计数
        Map<String, Integer> provinceCount = new LinkedHashMap<>();
        for (User u : all) {
            String province = getProvinceByCity(u.getCity());
            if (province != null) {
                provinceCount.merge(province, 1, Integer::sum);
            }
        }

        // 转为 VO 并按人数降序排列
        return provinceCount.entrySet().stream()
                .map(e -> new ProvinceDistributionVO(e.getKey(), e.getValue()))
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    /** 根据城市名获取省份名 */
    private String getProvinceByCity(String city) {
        if (city == null || city.isBlank()) return null;

        // 直辖市
        if (city.contains("北京")) return "北京";
        if (city.contains("上海")) return "上海";
        if (city.contains("天津")) return "天津";
        if (city.contains("重庆")) return "重庆";

        // 省份映射
        String[][] mappings = {
                // 华东
                {"杭州", "宁波", "温州", "嘉兴", "湖州", "绍兴", "金华", "衢州", "舟山", "台州", "丽水", "浙江"},
                {"南京", "无锡", "徐州", "常州", "苏州", "南通", "连云港", "淮安", "盐城", "扬州", "镇江", "泰州", "宿迁", "江苏"},
                {"合肥", "芜湖", "蚌埠", "淮南", "马鞍山", "淮北", "铜陵", "安庆", "黄山", "滁州", "阜阳", "宿州", "六安", "亳州", "池州", "宣城", "安徽"},
                {"福州", "厦门", "莆田", "三明", "泉州", "漳州", "南平", "龙岩", "宁德", "福建"},
                {"南昌", "景德镇", "萍乡", "九江", "新余", "鹰潭", "赣州", "吉安", "宜春", "抚州", "上饶", "江西"},
                {"济南", "青岛", "淄博", "枣庄", "东营", "烟台", "潍坊", "济宁", "泰安", "威海", "日照", "临沂", "德州", "聊城", "滨州", "菏泽", "山东"},

                // 华南
                {"广州", "韶关", "深圳", "珠海", "汕头", "佛山", "江门", "湛江", "茂名", "肇庆", "惠州", "梅州", "汕尾", "河源", "阳江", "清远", "东莞", "中山", "潮州", "揭阳", "云浮", "广东"},
                {"南宁", "柳州", "桂林", "梧州", "北海", "防城港", "钦州", "贵港", "玉林", "百色", "贺州", "河池", "来宾", "崇左", "广西"},
                {"海口", "三亚", "三沙", "儋州", "海南"},

                // 华中
                {"郑州", "开封", "洛阳", "平顶山", "安阳", "鹤壁", "新乡", "焦作", "濮阳", "许昌", "漯河", "三门峡", "南阳", "商丘", "信阳", "周口", "驻马店", "河南"},
                {"武汉", "黄石", "十堰", "宜昌", "襄阳", "鄂州", "荆门", "孝感", "荆州", "黄冈", "咸宁", "随州", "恩施", "湖北"},
                {"长沙", "株洲", "湘潭", "衡阳", "邵阳", "岳阳", "常德", "张家界", "益阳", "郴州", "永州", "怀化", "娄底", "湖南"},

                // 华北
                {"石家庄", "唐山", "秦皇岛", "邯郸", "邢台", "保定", "张家口", "承德", "沧州", "廊坊", "衡水", "河北"},
                {"太原", "大同", "阳泉", "长治", "晋城", "朔州", "晋中", "运城", "忻州", "临汾", "吕梁", "山西"},
                {"呼和浩特", "包头", "乌海", "赤峰", "通辽", "鄂尔多斯", "呼伦贝尔", "巴彦淖尔", "乌兰察布", "兴安", "锡林郭勒", "阿拉善", "内蒙古"},

                // 东北
                {"沈阳", "大连", "鞍山", "抚顺", "本溪", "丹东", "锦州", "营口", "阜新", "辽阳", "盘锦", "铁岭", "朝阳", "葫芦岛", "辽宁"},
                {"长春", "吉林", "四平", "辽源", "通化", "白山", "松原", "白城", "延边", "吉林"},
                {"哈尔滨", "齐齐哈尔", "鸡西", "鹤岗", "双鸭山", "大庆", "伊春", "佳木斯", "七台河", "牡丹江", "黑河", "绥化", "大兴安岭", "黑龙江"},

                // 西北
                {"西安", "铜川", "宝鸡", "咸阳", "渭南", "延安", "汉中", "榆林", "安康", "商洛", "陕西"},
                {"兰州", "嘉峪关", "金昌", "白银", "天水", "武威", "张掖", "平凉", "酒泉", "庆阳", "定西", "陇南", "临夏", "甘南", "甘肃"},
                {"西宁", "海东", "海北", "黄南", "海南", "果洛", "玉树", "海西", "青海"},
                {"银川", "石嘴山", "吴忠", "固原", "中卫", "宁夏"},
                {"乌鲁木齐", "克拉玛依", "吐鲁番", "哈密", "昌吉", "博尔塔拉", "巴音郭楞", "阿克苏", "克孜勒苏", "喀什", "和田", "伊犁", "塔城", "阿勒泰", "新疆"},

                // 西南
                {"成都", "自贡", "攀枝花", "泸州", "德阳", "绵阳", "广元", "遂宁", "内江", "乐山", "南充", "眉山", "宜宾", "广安", "达州", "雅安", "巴中", "资阳", "阿坝", "甘孜", "凉山", "四川"},
                {"贵阳", "六盘水", "遵义", "安顺", "毕节", "铜仁", "黔西南", "黔东南", "黔南", "贵州"},
                {"昆明", "曲靖", "玉溪", "保山", "昭通", "丽江", "普洱", "临沧", "楚雄", "红河", "文山", "西双版纳", "大理", "德宏", "怒江", "迪庆", "云南"},
                {"拉萨", "日喀则", "昌都", "林芝", "山南", "那曲", "阿里", "西藏"},
        };

        for (String[] mapping : mappings) {
            for (int i = 0; i < mapping.length - 1; i++) {
                if (city.contains(mapping[i])) {
                    return mapping[mapping.length - 1];
                }
            }
        }

        return null;
    }
}

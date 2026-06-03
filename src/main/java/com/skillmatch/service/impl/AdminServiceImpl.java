package com.skillmatch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skillmatch.context.UserContext;
import com.skillmatch.domain.dto.*;
import com.skillmatch.domain.po.*;
import com.skillmatch.domain.query.AdminUserQuery;
import com.skillmatch.domain.vo.*;
import com.skillmatch.enums.ErrorCode;
import com.skillmatch.exceptions.BusinessException;
import com.skillmatch.mapper.*;
import com.skillmatch.service.IAdminService;
import com.skillmatch.utils.GeoUtil;
import com.skillmatch.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Comparator;

import static com.skillmatch.constants.FinalConstant.AUTH_EXPIRE;
import static com.skillmatch.constants.RedisConstant.LOGIN_TOKEN_KEY;
import static com.skillmatch.constants.RedisConstant.USER_LOCATION_KEY;

/**
 * 管理端服务实现
 * <p>管理员身份：admin_user 表（独立于 user 表）</p>
 * <p>层级：ROOT 可管理子管理员，ADMIN 不可</p>
 * <p>操作日志：SLF4J [ADMIN] 前缀</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<UserMapper, User> implements IAdminService {

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final UserSkillMapper userSkillMapper;
    private final UserHobbyMapper userHobbyMapper;
    private final SkillTagMapper skillTagMapper;
    private final PostMapper postMapper;
    private final PostTagMapper postTagMapper;
    private final PostCommentMapper postCommentMapper;
    private final LikeInfoMapper likeInfoMapper;
    private final ContactRequestMapper contactRequestMapper;
    private final NotificationMapper notificationMapper;
    private final UserGalleryMapper userGalleryMapper;
    private final FriendMapper friendMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final StringRedisTemplate redisTemplate;
    private final GeoUtil geoUtil;

    // ==================== 权限校验 ====================

    /** 校验当前用户是否为管理员，返回 admin_user 记录 */
    private AdminUser checkAdmin() {
        String userId = UserContext.getUserId();
        if (userId == null) throw new BusinessException(ErrorCode.NOT_LOGIN);
        AdminUser au = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUserId, userId));
        if (au == null || au.getStatus() == null || au.getStatus() == 0)
            throw new BusinessException(ErrorCode.NOT_AUTH, "无管理员权限或已被禁用");
        return au;
    }

    /** 校验是否为根管理员，是则返回 admin_user 记录 */
    private AdminUser checkRootAdmin() {
        AdminUser au = checkAdmin();
        if (!"ROOT".equals(au.getRole()))
            throw new BusinessException(ErrorCode.NOT_AUTH, "仅根管理员可执行此操作");
        return au;
    }

    // ==================== 登录 ====================

    @Override
    public Map<String, Object> login(AdminLoginDTO dto) {
        // 查 admin_user 表
        AdminUser au = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUserId, dto.getUserId()));
        if (au == null)
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号或密码错误");

        // 校验密码
        if (!SecureUtil.md5(dto.getPassword()).equals(au.getPassword()))
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号或密码错误");

        // 检查状态
        if (au.getStatus() == null || au.getStatus() == 0)
            throw new BusinessException(ErrorCode.NOT_AUTH, "账号已被禁用");

        // 生成 JWT
        String token = JwtUtil.createToken(au.getUserId(), au.getName());
        redisTemplate.opsForValue().set(LOGIN_TOKEN_KEY + au.getUserId(), token, AUTH_EXPIRE, TimeUnit.DAYS);

        // 更新最后登录时间
        au.setUpdatedAt(LocalDateTime.now());
        adminUserMapper.updateById(au);

        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        Map<String, Object> admin = new HashMap<>();
        admin.put("id", au.getId());
        admin.put("userId", au.getUserId());
        admin.put("name", au.getName());
        admin.put("role", au.getRole());
        map.put("admin", admin);

        log.info("[ADMIN] LOGIN | userId={} role={}", au.getUserId(), au.getRole());
        return map;
    }

    // ==================== 用户列表 ====================

    @Override
    public Page<AdminUserVO> listUsers(AdminUserQuery q) {
        AdminUser au = checkAdmin();
        log.info("[ADMIN] LIST_USERS | admin={} role={} | filters={}", au.getUserId(), au.getRole(), q);

        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(q.getQuickFilter())) {
            switch (q.getQuickFilter()) {
                case "all_real" -> w.eq(User::getRobot, false).or().isNull(User::getRobot);
                case "all_robot" -> w.eq(User::getRobot, true);
                case "all_frozen" -> w.eq(User::getStatus, 2);
                case "pending_unfreeze", "no_coords" -> w.and(x -> x.isNull(User::getLatitude).or()
                        .isNull(User::getLongitude).or().eq(User::getLatitude, 0.0).or().eq(User::getLongitude, 0.0));
            }
        }

        if (StrUtil.isNotBlank(q.getUserId())) w.like(User::getUserId, q.getUserId());
        if (StrUtil.isNotBlank(q.getName())) w.like(User::getName, q.getName());
        if (q.getStatus() != null) w.eq(User::getStatus, q.getStatus());
        if (q.getRobot() != null) w.eq(User::getRobot, q.getRobot());
        if (StrUtil.isNotBlank(q.getCity())) w.eq(User::getCity, q.getCity());
        if (q.getCreatedAtStart() != null) w.ge(User::getCreatedAt, q.getCreatedAtStart());
        if (q.getCreatedAtEnd() != null) w.le(User::getCreatedAt, q.getCreatedAtEnd());

        if (StrUtil.isNotBlank(q.getSkillTag())) {
            Set<String> ids = userSkillMapper.selectList(
                    new LambdaQueryWrapper<UserSkill>().like(UserSkill::getSkillName, q.getSkillTag()))
                    .stream().map(UserSkill::getUserId).collect(Collectors.toSet());
            if (ids.isEmpty()) return emptyPage(q);
            w.in(User::getUserId, ids);
        }

        boolean geoFilter = q.getCenterLat() != null && q.getCenterLng() != null
                && q.getRadiusKm() != null && q.getRadiusKm() > 0;

        w.orderByDesc(User::getCreatedAt);
        Page<User> page = page(new Page<>(q.getPage(), geoFilter ? 10000 : q.getSize()), w);

        List<User> records = page.getRecords();

        if (geoFilter) {
            records = records.stream()
                    .filter(u -> haversine(q.getCenterLat(), q.getCenterLng(),
                            u.getLatitude(), u.getLongitude()) <= q.getRadiusKm())
                    .collect(Collectors.toList());
            int from = (q.getPage() - 1) * q.getSize();
            int to = Math.min(from + q.getSize(), records.size());
            records = from >= records.size() ? Collections.emptyList() : records.subList(from, to);
            page = new Page<>(q.getPage(), q.getSize(), records.size());
        }

        List<AdminUserVO> vos = records.stream().map(this::toVO).collect(Collectors.toList());
        Page<AdminUserVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(vos);
        return result;
    }

    private Page<AdminUserVO> emptyPage(AdminUserQuery q) {
        Page<AdminUserVO> p = new Page<>(q.getPage(), q.getSize());
        p.setTotal(0); p.setRecords(Collections.emptyList());
        return p;
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private AdminUserVO toVO(User u) {
        AdminUserVO vo = new AdminUserVO();
        BeanUtil.copyProperties(u, vo);
        List<UserSkill> skills = userSkillMapper.selectList(
                new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, u.getUserId()));
        List<UserHobby> hobbies = userHobbyMapper.selectList(
                new LambdaQueryWrapper<UserHobby>().eq(UserHobby::getUserId, u.getUserId()));
        vo.setSkillTags(skills.stream().map(UserSkill::getSkillName).collect(Collectors.joining(",")));
        vo.setHobbyTags(hobbies.stream().map(UserHobby::getHobbyName).collect(Collectors.joining(",")));
        return vo;
    }

    // ==================== 用户详情 ====================

    @Override
    public AdminUserVO getUserDetail(String userId) {
        checkAdmin();
        User u = lambdaQuery().eq(User::getUserId, userId).one();
        if (u == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        return toVO(u);
    }

    // ==================== 新增/编辑用户 ====================

    @Override
    @Transactional
    public void createUser(AdminUserSaveDTO dto) {
        AdminUser au = checkAdmin();
        String uid = StrUtil.isBlank(dto.getUserId())
                ? "user_" + System.currentTimeMillis() + "_" + RandomUtil.randomNumbers(4)
                : dto.getUserId();

        if (lambdaQuery().eq(User::getUserId, uid).count() > 0)
            throw new BusinessException(ErrorCode.DUPLICATE, "用户ID已存在");

        User u = new User();
        u.setUserId(uid); u.setName(dto.getName());
        u.setPassword(SecureUtil.md5(StrUtil.isBlank(dto.getPassword()) ? "123456" : dto.getPassword()));
        u.setContactInfo(dto.getContactInfo()); u.setAvatarUrl(dto.getAvatarUrl());
        u.setSignature(dto.getSignature()); u.setBio(dto.getBio());
        u.setLatitude(dto.getLatitude() != null ? dto.getLatitude() : 0.0);
        u.setLongitude(dto.getLongitude() != null ? dto.getLongitude() : 0.0);
        u.setCity(dto.getCity());
        u.setRobot(Boolean.TRUE.equals(dto.getRobot()));
        u.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        u.setCreatedAt(LocalDateTime.now()); u.setUpdatedAt(LocalDateTime.now());
        u.setLikeCount(0); u.setPostCount(0);
        save(u);

        if (u.getLatitude() != 0.0 && u.getLongitude() != 0.0)
            redisTemplate.opsForGeo().add(USER_LOCATION_KEY, new Point(u.getLongitude(), u.getLatitude()), uid);

        log.info("[ADMIN] CREATE_USER | admin={} role={} | target={} | name={} | robot={}",
                au.getUserId(), au.getRole(), uid, u.getName(), u.getRobot());
    }

    @Override
    @Transactional
    public void updateUser(String userId, AdminUserSaveDTO dto) {
        AdminUser au = checkAdmin();
        User u = lambdaQuery().eq(User::getUserId, userId).one();
        if (u == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");

        var uw = lambdaUpdate().eq(User::getUserId, userId);
        if (StrUtil.isNotBlank(dto.getName())) uw.set(User::getName, dto.getName());
        if (StrUtil.isNotBlank(dto.getPassword())) uw.set(User::getPassword, SecureUtil.md5(dto.getPassword()));
        if (dto.getContactInfo() != null) uw.set(User::getContactInfo, dto.getContactInfo());
        if (dto.getAvatarUrl() != null) uw.set(User::getAvatarUrl, dto.getAvatarUrl());
        if (dto.getSignature() != null) uw.set(User::getSignature, dto.getSignature());
        if (dto.getBio() != null) uw.set(User::getBio, dto.getBio());
        if (dto.getLatitude() != null) uw.set(User::getLatitude, dto.getLatitude());
        if (dto.getLongitude() != null) uw.set(User::getLongitude, dto.getLongitude());
        if (dto.getCity() != null) uw.set(User::getCity, dto.getCity());
        if (dto.getRobot() != null) uw.set(User::getRobot, dto.getRobot());
        if (dto.getStatus() != null) uw.set(User::getStatus, dto.getStatus());
        uw.set(User::getUpdatedAt, LocalDateTime.now()).update();

        double lat = dto.getLatitude() != null ? dto.getLatitude() : u.getLatitude();
        double lng = dto.getLongitude() != null ? dto.getLongitude() : u.getLongitude();
        if (lat != 0.0 && lng != 0.0) {
            redisTemplate.opsForGeo().remove(USER_LOCATION_KEY, userId);
            redisTemplate.opsForGeo().add(USER_LOCATION_KEY, new Point(lng, lat), userId);
        }
        log.info("[ADMIN] UPDATE_USER | admin={} role={} | target={}", au.getUserId(), au.getRole(), userId);
    }

    // ==================== 冻结/解冻 ====================

    @Override
    @Transactional
    public void updateUserStatus(UserStatusDTO dto) {
        AdminUser au = checkAdmin();
        User target = lambdaQuery().eq(User::getUserId, dto.getUserId()).one();
        if (target == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");

        // 不能冻结管理员
        Long adminCount = adminUserMapper.selectCount(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUserId, dto.getUserId()));
        if (dto.getStatus() == 2 && adminCount > 0)
            throw new BusinessException(ErrorCode.NOT_AUTH, "不能冻结管理员账号");

        lambdaUpdate().eq(User::getUserId, dto.getUserId())
                .set(User::getStatus, dto.getStatus())
                .set(User::getUpdatedAt, LocalDateTime.now()).update();

        String action = dto.getStatus() == 2 ? "FREEZE" : "UNFREEZE";
        if (dto.getStatus() == 2)
            redisTemplate.delete(LOGIN_TOKEN_KEY + dto.getUserId());

        log.info("[ADMIN] {} | admin={} role={} | target={}", action, au.getUserId(), au.getRole(), dto.getUserId());
    }

    // ==================== 删除机器人 ====================

    @Override
    @Transactional
    public void deleteRobot(String userId) {
        AdminUser au = checkAdmin();
        User u = lambdaQuery().eq(User::getUserId, userId).one();
        if (u == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        doCascadeDelete(Collections.singletonList(userId));
        log.info("[ADMIN] DELETE_USER | admin={} role={} | target={}", au.getUserId(), au.getRole(), userId);
    }

    @Override
    @Transactional
    public void batchDeleteRobots(List<String> userIds) {
        AdminUser au = checkAdmin();
        if (userIds == null || userIds.isEmpty()) throw new BusinessException(ErrorCode.PARAM_ERROR, "ID列表为空");
        doCascadeDelete(userIds);
        log.info("[ADMIN] BATCH_DELETE | admin={} role={} | count={} | targets={}",
                au.getUserId(), au.getRole(), userIds.size(), userIds);
    }


    // 批量删除
    private void doCascadeDelete(List<String> ids) {
        List<String> postIds = postMapper.selectList(
                new LambdaQueryWrapper<Post>().in(Post::getAuthorId, ids))
                .stream().map(Post::getId).toList();
        if (!postIds.isEmpty()) {
            postTagMapper.delete(new LambdaQueryWrapper<PostTag>().in(PostTag::getPostId, postIds));
            postCommentMapper.delete(new LambdaQueryWrapper<PostComment>().in(PostComment::getPostId, postIds));
            likeInfoMapper.delete(new LambdaQueryWrapper<LikeInfo>().in(LikeInfo::getBizId, postIds));
        }
        postMapper.delete(new LambdaQueryWrapper<Post>().in(Post::getAuthorId, ids));
        postCommentMapper.delete(new LambdaQueryWrapper<PostComment>().in(PostComment::getUserId, ids));
        likeInfoMapper.delete(new LambdaQueryWrapper<LikeInfo>().in(LikeInfo::getUserId, ids));
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .in(Notification::getActorId, ids).or().in(Notification::getReceiverId, ids));
        contactRequestMapper.delete(new LambdaQueryWrapper<ContactRequest>()
                .in(ContactRequest::getFromUserId, ids).or().in(ContactRequest::getToUserId, ids));
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .in(Friend::getUserId, ids).or().in(Friend::getFriendId, ids));
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .in(ChatMessage::getFromUserId, ids).or().in(ChatMessage::getToUserId, ids));
        userSkillMapper.delete(new LambdaQueryWrapper<UserSkill>().in(UserSkill::getUserId, ids));
        userHobbyMapper.delete(new LambdaQueryWrapper<UserHobby>().in(UserHobby::getUserId, ids));
        userGalleryMapper.delete(new LambdaQueryWrapper<UserGallery>().in(UserGallery::getUserId, ids));
        userMapper.delete(new LambdaQueryWrapper<User>().in(User::getUserId, ids));
        for (String id : ids) {
            redisTemplate.opsForGeo().remove(USER_LOCATION_KEY, id);
            redisTemplate.delete(LOGIN_TOKEN_KEY + id);
        }
    }

    // ==================== 批量创建机器人 ====================

    @Override
    @Transactional
    public int batchCreateRobots(BatchRobotDTO dto) {
        AdminUser au = checkAdmin();
        long ts = System.currentTimeMillis();
        int n = 0;
        for (int i = 0; i < dto.getCount(); i++) {
            String uid = "bot_" + ts + "_" + RandomUtil.randomNumbers(6) + "_" + i;
            User bot = new User();
            bot.setUserId(uid); bot.setName(dto.getNamePrefix() + String.format("%03d", i + 1));
            bot.setPassword(SecureUtil.md5("123456")); bot.setRobot(true); bot.setStatus(1);
            bot.setSignature("我是" + dto.getNamePrefix() + String.format("%03d", i + 1));
            bot.setBio("系统机器人"); bot.setLikeCount(RandomUtil.randomInt(0, 50)); bot.setPostCount(0);
            bot.setCreatedAt(LocalDateTime.now()); bot.setUpdatedAt(LocalDateTime.now());

            if (dto.getCenterLat() != null && dto.getCenterLng() != null
                    && dto.getSpreadKm() != null && dto.getSpreadKm() > 0) {
                double[] c = randomOffset(dto.getCenterLat(), dto.getCenterLng(), dto.getSpreadKm());
                bot.setLatitude(c[0]); bot.setLongitude(c[1]);
                // 自动从坐标解析城市名，确保管理端地图有数据
                String city = geoUtil.resolveCity(c[1], c[0]);
                bot.setCity(city);
            } else { bot.setLatitude(0.0); bot.setLongitude(0.0); }

            save(bot); n++;

            if (bot.getLatitude() != 0.0 && bot.getLongitude() != 0.0)
                redisTemplate.opsForGeo().add(USER_LOCATION_KEY, new Point(bot.getLongitude(), bot.getLatitude()), uid);

            if (dto.getSkillTags() != null) for (String tag : dto.getSkillTags()) {
                UserSkill s = new UserSkill(); s.setUserId(uid); s.setSkillName(tag);
                s.setSkillType(1); s.setCreatedTime(LocalDateTime.now()); userSkillMapper.insert(s);
            }
            if (dto.getHobbyTags() != null) for (String tag : dto.getHobbyTags()) {
                UserHobby h = new UserHobby(); h.setUserId(uid);
                h.setHobbyName(tag); h.setCreateAt(LocalDateTime.now()); userHobbyMapper.insert(h);
            }
        }
        log.info("[ADMIN] CREATE_ROBOTS | admin={} role={} | count={} | prefix={}",
                au.getUserId(), au.getRole(), n, dto.getNamePrefix());
        return n;
    }

    private double[] randomOffset(double clat, double clng, double rKm) {
        double angle = RandomUtil.randomDouble(0, 2 * Math.PI);
        double dist = RandomUtil.randomDouble(0, rKm);
        double dLat = (dist / 111.0) * Math.cos(angle);
        double dLng = (dist / (111.0 * Math.cos(Math.toRadians(clat)))) * Math.sin(angle);
        return new double[]{ round6(clat + dLat), round6(clng + dLng) };
    }

    private double round6(double v) { return Math.round(v * 1_000_000.0) / 1_000_000.0; }

    // ==================== 数据统计 ====================

    @Override
    public DashboardVO getDashboard() {
        checkAdmin();
        DashboardVO vo = new DashboardVO();
        vo.setTotalUsers(lambdaQuery().count());
        vo.setRealUsers(lambdaQuery().eq(User::getRobot, false).or().isNull(User::getRobot).count());
        vo.setRobotUsers(lambdaQuery().eq(User::getRobot, true).count());
        vo.setNormalUsers(lambdaQuery().eq(User::getStatus, 1).or().isNull(User::getStatus).count());
        vo.setFrozenUsers(lambdaQuery().eq(User::getStatus, 2).count());

        long t = vo.getTotalUsers();
        vo.setRealPercent(t > 0 ? pct(vo.getRealUsers(), t) : 0.0);
        vo.setRobotPercent(t > 0 ? pct(vo.getRobotUsers(), t) : 0.0);
        vo.setNormalPercent(t > 0 ? pct(vo.getNormalUsers(), t) : 0.0);
        vo.setFrozenPercent(t > 0 ? pct(vo.getFrozenUsers(), t) : 0.0);

        vo.setDailyNewUsers(getDailyStats(7));
        //城市分布
        vo.setCityDistribution(cityDistribution());
        vo.setTagDistribution(tagDistribution());
        vo.setCanSkillDistribution(skillTypeDistribution(1));
        vo.setWantSkillDistribution(skillTypeDistribution(2));
        vo.setHobbyDistribution(hobbyDistribution());
        return vo;
    }

    private double pct(long part, long total) { return Math.round(part * 10000.0 / total) / 100.0; }

    @Override
    public List<DailyStatsVO> getDailyStats(Integer days) {
        if (days == null || days <= 0) days = 7;
        List<DailyStatsVO> stats = new ArrayList<>();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            LocalDateTime s = d.atStartOfDay(), e = d.plusDays(1).atStartOfDay();
            long real = lambdaQuery().ge(User::getCreatedAt, s).lt(User::getCreatedAt, e)
                    .and(w -> w.eq(User::getRobot, false).or().isNull(User::getRobot)).count();
            long robot = lambdaQuery().ge(User::getCreatedAt, s).lt(User::getCreatedAt, e)
                    .eq(User::getRobot, true).count();
            stats.add(new DailyStatsVO(d.format(f), real + robot, real, robot));
        }
        return stats;
    }

    private List<CityStatsVO> cityDistribution() {
        List<User> all = lambdaQuery().isNotNull(User::getCity).ne(User::getCity, "")
                .ne(User::getCity, "未知").list();
        Map<String, long[]> m = new LinkedHashMap<>();
        for (User u : all) {
            if (!GeoUtil.KNOWN_CITIES.contains(u.getCity())) continue;
            m.putIfAbsent(u.getCity(), new long[2]);
            if (Boolean.TRUE.equals(u.getRobot())) m.get(u.getCity())[1]++;
            else m.get(u.getCity())[0]++;
        }
        return m.entrySet().stream()
                .map(e -> new CityStatsVO(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[0] + e.getValue()[1]))
                .sorted((a, b) -> Long.compare(b.getTotal(), a.getTotal())).collect(Collectors.toList());
    }

    private List<TagStatsVO> tagDistribution() {
        List<SkillTag> tags = skillTagMapper.selectList(null);
        List<UserSkill> uss = userSkillMapper.selectList(null);
        Set<String> bots = lambdaQuery().eq(User::getRobot, true).list()
                .stream().map(User::getUserId).collect(Collectors.toSet());
        List<TagStatsVO> r = new ArrayList<>();
        for (SkillTag t : tags) {
            long rc = 0, bc = 0;
            for (UserSkill us : uss)
                if (us.getSkillName().equals(t.getName())) {
                    if (bots.contains(us.getUserId())) bc++; else rc++;
                }
            if (rc + bc > 0) r.add(new TagStatsVO(t.getName(), bc, rc, rc + bc));
        }
        r.sort((a, b) -> Long.compare(b.getRobotCount(), a.getRobotCount()));
        return r;
    }

    /** 按技能类型统计：1=能教 2=想学 */
    private List<TagStatsVO> skillTypeDistribution(int skillType) {
        List<UserSkill> skills = userSkillMapper.selectList(
                new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getSkillType, skillType));
        Map<String, Long> counter = new LinkedHashMap<>();
        for (UserSkill s : skills) {
            counter.merge(s.getSkillName(), 1L, Long::sum);
        }
        return counter.entrySet().stream()
                .map(e -> new TagStatsVO(e.getKey(), 0L, e.getValue(), e.getValue()))
                .sorted(Comparator.comparingLong(TagStatsVO::getTotal).reversed())
                .limit(8).collect(Collectors.toList());
    }

    /** 兴趣爱好分布 */
    private List<TagStatsVO> hobbyDistribution() {
        List<UserHobby> hobbies = userHobbyMapper.selectList(null);
        Map<String, Long> counter = new LinkedHashMap<>();
        for (UserHobby h : hobbies) {
            counter.merge(h.getHobbyName(), 1L, Long::sum);
        }
        return counter.entrySet().stream()
                .map(e -> new TagStatsVO(e.getKey(), 0L, e.getValue(), e.getValue()))
                .sorted(Comparator.comparingLong(TagStatsVO::getTotal).reversed())
                .limit(8).collect(Collectors.toList());
    }

    // ==================== 管理员管理（仅 ROOT） ====================

    @Override
    public List<AdminInfoVO> listAdmins() {
        checkAdmin(); // 所有管理员都可以看列表
        List<AdminUser> list = adminUserMapper.selectList(
                new LambdaQueryWrapper<AdminUser>().orderByAsc(AdminUser::getRole).orderByDesc(AdminUser::getCreatedAt));

        // 收集 parent 名称
        Map<Long, String> nameMap = list.stream()
                .collect(Collectors.toMap(AdminUser::getId, AdminUser::getName, (a, b) -> a));

        return list.stream().map(a -> {
            AdminInfoVO vo = new AdminInfoVO();
            BeanUtil.copyProperties(a, vo);
            if (a.getParentId() != null) vo.setParentName(nameMap.getOrDefault(a.getParentId(), "-"));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addAdmin(AddAdminDTO dto) {
        AdminUser root = checkRootAdmin();

        // 检查 user 是否存在
        if (!lambdaQuery().eq(User::getUserId, dto.getUserId()).exists())
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");

        // 检查是否已是管理员
        Long exists = adminUserMapper.selectCount(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUserId, dto.getUserId()));
        if (exists > 0)
            throw new BusinessException(ErrorCode.DUPLICATE, "该用户已是管理员");

        AdminUser au = new AdminUser();
        au.setUserId(dto.getUserId());
        au.setName(dto.getName());
        au.setRole("ADMIN");
        au.setParentId(root.getId());
        au.setStatus(1);
        au.setCreatedAt(LocalDateTime.now());
        au.setUpdatedAt(LocalDateTime.now());
        adminUserMapper.insert(au);

        log.info("[ADMIN] ADD_ADMIN | root={} | target={} name={}", root.getUserId(), dto.getUserId(), dto.getName());
    }

    @Override
    @Transactional
    public void toggleAdminStatus(Long adminId) {
        AdminUser root = checkRootAdmin();
        AdminUser target = adminUserMapper.selectById(adminId);
        if (target == null) throw new BusinessException(ErrorCode.NOT_FOUND, "管理员不存在");
        if ("ROOT".equals(target.getRole()))
            throw new BusinessException(ErrorCode.NOT_AUTH, "不能禁用根管理员");

        int newStatus = target.getStatus() == 1 ? 0 : 1;
        target.setStatus(newStatus);
        target.setUpdatedAt(LocalDateTime.now());
        adminUserMapper.updateById(target);

        log.info("[ADMIN] TOGGLE_ADMIN | root={} | target={} status={}",
                root.getUserId(), target.getUserId(), newStatus);
    }

    @Override
    @Transactional
    public void removeAdmin(Long adminId) {
        AdminUser root = checkRootAdmin();
        AdminUser target = adminUserMapper.selectById(adminId);
        if (target == null) throw new BusinessException(ErrorCode.NOT_FOUND, "管理员不存在");
        if ("ROOT".equals(target.getRole()))
            throw new BusinessException(ErrorCode.NOT_AUTH, "不能删除根管理员");

        adminUserMapper.deleteById(adminId);

        log.info("[ADMIN] REMOVE_ADMIN | root={} | target={}", root.getUserId(), target.getUserId());
    }
}

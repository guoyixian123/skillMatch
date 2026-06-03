package com.skillmatch.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skillmatch.domain.dto.*;
import com.skillmatch.domain.query.AdminUserQuery;
import com.skillmatch.domain.vo.*;
import com.skillmatch.service.IAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端 REST 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;

    /** 管理员登录 */
    @PostMapping("/login")
    public RESTful<Map<String, Object>> login(@Valid @RequestBody AdminLoginDTO dto) {
        return RESTful.success(adminService.login(dto));
    }

    /** 用户列表（分页 + 多条件筛选 + 地理范围） */
    @GetMapping("/users")
    public RESTful<PageVO<AdminUserVO>> listUsers(AdminUserQuery query) {
        Page<AdminUserVO> page = adminService.listUsers(query);
        return RESTful.success(PageVO.of(page, page.getRecords()));
    }

    /** 用户详情 */
    @GetMapping("/users/{userId}")
    public RESTful<AdminUserVO> getUserDetail(@PathVariable String userId) {
        return RESTful.success(adminService.getUserDetail(userId));
    }

    /** 新增用户 */
    @PostMapping("/users")
    public RESTful<Object> createUser(@Valid @RequestBody AdminUserSaveDTO dto) {
        adminService.createUser(dto);
        return RESTful.success(null, "用户创建成功");
    }

    /** 编辑用户 */
    @PutMapping("/users/{userId}")
    public RESTful<Object> updateUser(@PathVariable String userId, @Valid @RequestBody AdminUserSaveDTO dto) {
        adminService.updateUser(userId, dto);
        return RESTful.success(null, "用户信息已更新");
    }

    /** 冻结/解冻 */
    @PutMapping("/users/status")
    public RESTful<Object> updateUserStatus(@Valid @RequestBody UserStatusDTO dto) {
        adminService.updateUserStatus(dto);
        return RESTful.success(null, dto.getStatus() == 2 ? "用户已冻结" : "用户已解冻");
    }

    /** 批量创建机器人 */
    @PostMapping("/robots/batch")
    public RESTful<Integer> batchCreateRobots(@Valid @RequestBody BatchRobotDTO dto) {
        int n = adminService.batchCreateRobots(dto);
        return RESTful.success(n, "成功创建 " + n + " 个机器人");
    }

    /** 删除单个机器人 */
    @DeleteMapping("/robots/{userId}")
    public RESTful<Object> deleteRobot(@PathVariable String userId) {
        adminService.deleteRobot(userId);
        return RESTful.success(null, "机器人已删除");
    }

    /** 批量删除机器人 */
    @DeleteMapping("/robots/batch")
    public RESTful<Object> batchDeleteRobots(@Valid @RequestBody BatchDeleteDTO dto) {
        adminService.batchDeleteRobots(dto.getUserIds());
        return RESTful.success(null, "已删除 " + dto.getUserIds().size() + " 个机器人");
    }

    /** 数据看板 */
    @GetMapping("/dashboard")
    public RESTful<DashboardVO> getDashboard() {
        return RESTful.success(adminService.getDashboard());
    }

    /** 每日新增（供 Excel 导出） */
    @GetMapping("/daily-stats")
    public RESTful<List<DailyStatsVO>> getDailyStats(@RequestParam(defaultValue = "30") Integer days) {
        return RESTful.success(adminService.getDailyStats(days));
    }

    // ==================== 管理员管理（仅 ROOT） ====================

    /** 管理员列表 */
    @GetMapping("/admins")
    public RESTful<List<AdminInfoVO>> listAdmins() {
        return RESTful.success(adminService.listAdmins());
    }

    /** 添加子管理员 */
    @PostMapping("/admins")
    public RESTful<Object> addAdmin(@Valid @RequestBody AddAdminDTO dto) {
        adminService.addAdmin(dto);
        return RESTful.success(null, "管理员添加成功");
    }

    /** 启用/禁用管理员 */
    @PutMapping("/admins/{adminId}/status")
    public RESTful<Object> toggleAdminStatus(@PathVariable Long adminId) {
        adminService.toggleAdminStatus(adminId);
        return RESTful.success(null, "状态已更新");
    }

    /** 删除管理员 */
    @DeleteMapping("/admins/{adminId}")
    public RESTful<Object> removeAdmin(@PathVariable Long adminId) {
        adminService.removeAdmin(adminId);
        return RESTful.success(null, "管理员已删除");
    }
}

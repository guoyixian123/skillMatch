package com.skillmatch.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skillmatch.domain.dto.*;
import com.skillmatch.domain.query.AdminUserQuery;
import com.skillmatch.domain.vo.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端服务接口
 */
public interface IAdminService {

    /** 分页查询用户列表 */
    Page<AdminUserVO> listUsers(AdminUserQuery query);

    /** 用户详情 */
    AdminUserVO getUserDetail(String userId);

    /** 新增用户 */
    void createUser(AdminUserSaveDTO dto);

    /** 编辑用户 */
    void updateUser(String userId, AdminUserSaveDTO dto);

    /** 冻结/解冻 */
    void updateUserStatus(UserStatusDTO dto);

    /** 删除机器人 */
    void deleteRobot(String userId);

    /** 批量删除机器人 */
    void batchDeleteRobots(List<String> userIds);

    /** 批量创建机器人 */
    int batchCreateRobots(BatchRobotDTO dto);

    /** 数据看板 */
    DashboardVO getDashboard();

    /** 管理员登录 */
    Map<String, Object> login(AdminLoginDTO dto);

    /** 每日新用户数据 */
    List<DailyStatsVO> getDailyStats(Integer days);

    // ===== 管理员管理（仅 ROOT 可操作） =====

    /** 管理员列表 */
    List<AdminInfoVO> listAdmins();

    /** 添加子管理员 */
    void addAdmin(AddAdminDTO dto);

    /** 启用/禁用管理员 */
    void toggleAdminStatus(Long adminId);

    /** 删除管理员 */
    void removeAdmin(Long adminId);
}

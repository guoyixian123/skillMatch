# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**SkillMatch** — 基于技能标签与地理位置的社交匹配平台，帮助用户发现身边志同道合的人。

- Group: `com.skillmatch`, artifact: `skillMatch`, version `0.0.1-SNAPSHOT`
- Java 21 + Spring Boot 3.3.2 + Maven
- MySQL 8.0 + Redis（缓存 / Token 存储）
- 前端: Vue 3 + Vite 5 + Element Plus + Pinia

## Build & Run

```bash
mvn compile          # 编译
mvn test             # 运行测试
mvn test -Dtest=Xxx  # 运行单个测试类
mvn package          # 打包
mvn clean            # 清理

# 启动后端（需要先配置环境变量，见 README.md）
mvn spring-boot:run   # 后端 → localhost:8080

# 启动前端
cd frontend
npm install
npm run dev           # 前端 → localhost:3000，API 自动代理到 8080
```

## 项目结构

```
skillMatch/
├── src/main/java/com/skillmatch/
│   ├── controller/         # REST 接口（16 个 Controller）
│   ├── service/            # 业务逻辑接口 + impl 实现
│   ├── mapper/             # MyBatis-Plus Mapper 接口（13 个）
│   ├── domain/
│   │   ├── po/             # 持久化实体（对应数据库表）
│   │   ├── dto/            # 请求 DTO
│   │   ├── vo/             # 响应 VO（含 RESTful<T> 统一封装）
│   │   └── query/          # 分页/查询参数对象
│   ├── config/             # Spring 配置（WebMvc、MyBatis-Plus、Geo）
│   ├── interceptor/        # JWT 认证拦截器
│   ├── utils/              # 工具类（JwtUtil、OssUtil）
│   ├── context/            # UserContext（ThreadLocal 存储当前用户ID）
│   ├── enums/              # ErrorCode 错误码枚举
│   ├── exceptions/         # BusinessException + GlobalExceptionHandler
│   ├── validator/          # 自定义校验器（TagValidator）
│   └── annotation/         # 自定义注解（@ValidTag）
├── src/main/resources/
│   ├── mapper/             # MyBatis XML 映射文件
│   └── application.yaml    # 主配置（通过环境变量注入敏感信息）
├── frontend/               # Vue 3 前端工程
│   └── src/
│       ├── views/          # 页面组件（17 个页面）
│       ├── stores/         # Pinia 状态管理（auth、chat、user）
│       ├── api/            # Axios 接口封装（7 个模块）
│       ├── router/         # 路由配置（含导航守卫）
│       ├── components/     # 公共组件（Navbar、MobileNav）
│       ├── utils/          # 工具函数（request 拦截器、avatar）
│       └── constants/      # 常量定义
├── ai-engine/              # AI 模块（FastAPI 骨架，开发中）
│   └── app.py
├── docs/                   # 文档（架构图、接口文档、OpenAPI 规范、部署指南）
├── init.sql                # 数据库初始化脚本（含所有建表语句）
└── test_data/              # 测试数据生成脚本
```

## 功能模块

| 模块 | Controller | 核心功能 |
|------|-----------|---------|
| 用户系统 | `UserController`、`AuthController` | 注册、登录、个人资料编辑、修改密码 |
| 技能标签 | `UserSkillController`、`TagController` | 个人技能添加/管理，标签分类 |
| 兴趣爱好 | `UserHobbyController` | 兴趣爱好标签管理 |
| 相册展示 | `UserGalleryController` | 图片上传至阿里云 OSS，个人相册管理 |
| 社区广场 | `PostController`、`PostCommentController`、`PostTagController` | 发帖、评论、点赞、标签筛选 |
| 好友系统 | `FriendController`、`ContactRequestController` | 好友申请、列表、管理 |
| 即时通讯 | `ChatController` | 好友间一对一聊天 |
| 发现匹配 | `MatchingController` | 基于地理位置的用户发现与技能匹配 |
| 通知系统 | `LikeNotificationController` | 点赞通知、好友申请通知 |

## 数据库表（14 张）

`user`、`user_skill`、`user_hobby`、`user_gallery`、`skill_tag`、`hobby_tag`、`post`、`post_comment`、`post_tag`、`friend`、`contact_request`、`chat_message`、`like_info`、`notification`

所有表主键均为 VARCHAR(64) 的雪花 ID 或 UUID，使用 utf8mb4 字符集。

## 关键架构约定

### 认证流程
1. 登录/注册返回 JWT token，前端存 localStorage，后续请求放入 `user_info` 请求头
2. `TokenJWTInterceptor` 拦截所有请求（排除 `/api/auth/register`、`/api/auth/login`）
3. 解析 token 拿到 userId，与 Redis 中存储的 token 比对校验
4. 校验通过后将 userId 写入 `UserContext`（ThreadLocal）

### API 响应规范
- 统一使用 `RESTful<T>` 封装：`{ code: 200, message: "success", data: T }`
- 成功：`RESTful.success(data)` → code=200
- 失败：`RESTful.error(code, message)` 或抛出 `BusinessException`

### 配置管理
- 敏感信息（数据库密码、API Key、OSS凭证等）通过**环境变量**注入，`application.yaml` 中使用 `${VAR:default}` 占位符
- 本地开发可复制 `application-example.yaml` 手动填写

### 前端路由
- 路由守卫 `router.beforeEach`：未登录重定向到 `/login`，已登录访问 guest 页面重定向到 `/discover`
- 默认首页 `/` → 重定向到 `/discover`
- 定位流程：浏览器 Geolocation API → IP 定位降级 → 提交到后端

### 前端状态管理
- `auth` store：token、user、地理位置（persistedstate 持久化到 localStorage）
- `chat` store：聊天消息管理
- `user` store：用户资料缓存

## 依赖速查

| 依赖 | 版本 | 用途 |
|------|------|------|
| spring-boot-starter-parent | 3.3.2 | Spring Boot 父工程 |
| mybatis-plus-spring-boot3-starter | 3.5.9 | ORM 框架 |
| mysql-connector-j | runtime | MySQL 驱动 |
| spring-boot-starter-data-redis | — | Redis 集成 |
| hutool-all | 5.8.44 | Java 工具包 |
| jjwt-api/impl/jackson | 0.13.0 | JWT 认证 |
| aliyun-sdk-oss | 3.18.4 | 阿里云对象存储 |
| lombok | optional | 代码生成 |
| Vue | ^3.4.0 | 前端框架 |
| Element Plus | ^2.7.0 | UI 组件库 |
| Pinia | ^2.1.0 | 状态管理 |
| Axios | ^1.7.0 | HTTP 客户端 |

## 注意事项
- 现在只修复管理端模块
- 关键部分需注释,生成代码时请使用中文注释
- 前端 Vite 开发服务器代理 `/api` 到 `localhost:8080`
- MyBatis-Plus 分页插件已配置（`MybatisPlusConfig`），使用 `Page<T>` 即可分页
- 图片上传限制 10MB（`application.yaml` 中 `spring.servlet.multipart` 配置）
- Mapper XML 文件在 `src/main/resources/mapper/` 下，与 Mapper 接口对应

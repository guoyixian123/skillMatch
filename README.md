# SkillMatch

> 基于技能标签与地理位置的社交匹配平台，帮助用户发现身边志同道合的人。

## 项目简介

SkillMatch 是一个面向技能型社交的全栈 Web 应用。用户可以展示自己的技能与兴趣爱好，通过地理位置发现附近的人，基于技能相似度进行匹配，还可以在社区广场发布动态、添加好友、即时聊天。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3 + Java 21 |
| ORM | MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0 + Redis 7.0 |
| 前端框架 | Vue 3 + Vite 5 |
| 状态管理 | Pinia + persistedstate |
| UI 组件库 | Element Plus |
| 认证 | JWT (jjwt 0.13) |
| 对象存储 | 阿里云 OSS |
| 地图服务 | 高德地图 API |
| 工具库 | Hutool 5.8 |

## 功能模块

| 模块 | 核心功能 |
|------|---------|
| **用户系统** | 注册、登录、个人资料编辑、修改密码 |
| **技能标签** | 添加/管理个人技能，标签分类展示 |
| **兴趣爱好** | 添加/管理兴趣爱好标签 |
| **相册展示** | 图片上传至 OSS，个人相册管理 |
| **社区广场** | 发帖、评论、点赞、标签筛选 |
| **好友系统** | 好友申请、好友列表、好友管理 |
| **即时通讯** | 好友间一对一实时聊天 |
| **发现匹配** | 基于地理位置的用户发现与技能匹配 |
| **通知系统** | 点赞通知、好友申请通知 |

## 项目结构

```
skillMatch/
├── src/main/java/com/skillmatch/
│   ├── controller/         # REST 接口（16 个 Controller）
│   ├── service/            # 业务逻辑接口 + impl 实现
│   ├── mapper/             # MyBatis-Plus Mapper 接口
│   ├── domain/
│   │   ├── po/             # 持久化实体（对应数据库表）
│   │   ├── dto/            # 请求 DTO
│   │   ├── vo/             # 响应 VO（含 RESTful<T> 统一封装）
│   │   └── query/          # 分页/查询参数对象
│   ├── config/             # Spring 配置（WebMvc、MyBatis-Plus、Geo）
│   ├── interceptor/        # JWT 认证拦截器
│   ├── utils/              # 工具类（JwtUtil、OssUtil）
│   ├── context/            # UserContext（ThreadLocal 存储当前用户 ID）
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
│       ├── stores/         # Pinia 状态管理
│       ├── api/            # Axios 接口封装
│       ├── router/         # 路由配置（含导航守卫）
│       ├── components/     # 公共组件
│       └── utils/          # 工具函数
├── ai-engine/              # AI 模块（FastAPI，开发中）
├── docs/                   # 文档（架构图、接口文档、OpenAPI 规范）
├── init.sql                # 数据库初始化脚本
└── test_data/              # 测试数据生成脚本
```

## 数据库设计

共 14 张表，所有主键均为 VARCHAR(64) 的雪花 ID / UUID，使用 utf8mb4 字符集。

| 表名 | 说明 |
|------|------|
| `user` | 用户基础信息（账号、密码、昵称、头像、地理位置等） |
| `user_skill` | 用户技能关联表 |
| `user_hobby` | 用户兴趣爱好关联表 |
| `user_gallery` | 用户相册图片 |
| `skill_tag` | 技能标签字典 |
| `hobby_tag` | 兴趣标签字典 |
| `post` | 社区广场帖子 |
| `post_comment` | 帖子评论 |
| `post_tag` | 帖子标签关联 |
| `friend` | 好友关系 |
| `contact_request` | 好友申请记录 |
| `chat_message` | 聊天消息 |
| `like_info` | 点赞记录 |
| `notification` | 通知消息 |

## 快速开始

### 环境要求

- **JDK** 21+
- **Maven** 3.8+
- **Node.js** 18+
- **MySQL** 8.0+
- **Redis** 7.0+

### 1. 克隆项目

```bash
git clone https://github.com/your-username/skillMatch.git
cd skillMatch
```

### 2. 初始化数据库

```bash
mysql -u root -p < init.sql
```

### 3. 配置环境变量

复制示例配置文件并填写：

```bash
cp application-example.yaml src/main/resources/application-local.yaml
```

或直接设置环境变量：

```bash
# MySQL
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_redis_password

# 高德地图
export GAODE_API_KEY=your_gaode_key

# 阿里云 OSS
export OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
export OSS_ACCESS_KEY_ID=your_oss_ak
export OSS_ACCESS_KEY_SECRET=your_oss_sk
export OSS_BUCKET_NAME=your_bucket

# JWT
export JWT_SECRET=your_jwt_secret
```

### 4. 启动后端

```bash
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:3000`，API 请求自动代理到后端 `8080` 端口。

### 6. 访问应用

打开浏览器访问 `http://localhost:3000`。

## 架构说明

### 认证流程

```
登录/注册 → 返回 JWT Token → 前端存入 localStorage
    ↓
后续请求携带 user_info 请求头
    ↓
TokenJWTInterceptor 拦截 → 解析 Token → Redis 校验
    ↓
校验通过 → userId 写入 UserContext (ThreadLocal)
```

### API 响应规范

统一使用 `RESTful<T>` 封装：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- 成功：`RESTful.success(data)` → code=200
- 失败：`RESTful.error(code, message)` 或抛出 `BusinessException`

### 前端路由守卫

- 未登录 → 重定向到 `/login`
- 已登录访问游客页面 → 重定向到 `/discover`
- 默认首页 `/` → 重定向到 `/discover`

## 常用命令

```bash
# 后端
mvn compile          # 编译
mvn test             # 运行测试
mvn test -Dtest=Xxx  # 运行单个测试类
mvn package          # 打包
mvn clean            # 清理

# 前端
cd frontend
npm run dev          # 开发模式
npm run build        # 生产构建
npm run preview      # 预览构建结果
```

## API 文档

接口文档见 `docs/` 目录：

- `docs/SkillMatch 接口文档.md` — Markdown 格式接口说明
- `docs/skillmatch-openapi.json` — OpenAPI 3.0 规范文件

## 许可证

本项目仅供学习交流使用。

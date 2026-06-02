# SkillMatch

基于技能标签与地理位置的社交匹配平台，帮助用户发现身边志同道合的人。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3 + Java 21 |
| ORM | MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0 + Redis |
| 前端框架 | Vue 3 + Vite 5 |
| 状态管理 | Pinia + persistedstate |
| UI 组件库 | Element Plus |
| 认证 | JWT |
| 对象存储 | 阿里云 OSS |
| 地图服务 | 高德地图 API |

## 功能模块

- **用户系统** — 注册、登录、个人资料编辑、修改密码
- **技能标签** — 添加/管理个人技能，标签分类展示
- **兴趣爱好** — 添加/管理兴趣爱好标签
- **相册展示** — 图片上传至 OSS，个人相册管理
- **社区广场** — 发帖、评论、点赞、标签筛选
- **好友系统** — 好友申请、好友列表、好友管理
- **即时通讯** — 好友间一对一聊天
- **发现匹配** — 基于地理位置的用户发现与技能匹配
- **通知系统** — 点赞通知、好友申请通知

## 项目结构

```
skillMatch/
├── src/main/java/com/skillmatch/
│   ├── controller/     # REST 接口
│   ├── mapper/         # MyBatis-Plus Mapper
│   ├── domain/         # 实体类、DTO、VO
│   ├── service/        # 业务逻辑
│   ├── config/         # Spring 配置
│   ├── utils/          # 工具类（JWT、OSS）
│   ├── context/        # 用户上下文
│   ├── enums/          # 错误码枚举
│   ├── exceptions/     # 全局异常处理
│   ├── validator/      # 自定义校验器
│   └── annotation/     # 自定义注解
├── src/main/resources/
│   ├── mapper/         # MyBatis XML
│   └── application.yaml
├── frontend/
│   ├── src/views/      # 页面组件
│   ├── src/stores/     # Pinia 状态管理
│   ├── src/api/        # 接口封装
│   ├── src/router/     # 路由配置
│   └── src/utils/      # 工具函数
├── init.sql            # 数据库初始化脚本
├── test_data/          # 测试数据生成脚本
└── ai-engine/          # AI 模块
```

## 快速开始

### 环境要求

- JDK 21
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+

### 1. 初始化数据库

```bash
mysql -u root -p < init.sql
```

### 2. 配置环境变量

```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_redis_password
export GAODE_API_KEY=your_gaode_key
export OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
export OSS_ACCESS_KEY_ID=your_oss_ak
export OSS_ACCESS_KEY_SECRET=your_oss_sk
export OSS_BUCKET_NAME=your_bucket
export JWT_SECRET=your_jwt_secret
```

### 3. 启动后端

```bash
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:3000`，API 请求自动代理到后端。

### 5. 访问应用

打开浏览器访问 `http://localhost:3000`。

## API 文档

接口文档见 `docs/SkillMatch 接口文档.md` 或 `docs/skillmatch-openapi.json`。

# SkillMatch

> 基于技能标签与地理位置的社交匹配平台，帮助用户发现身边志同道合的人。

## 项目简介

SkillMatch 是一个面向技能型社交的全栈 Web 应用。用户可以展示自己的技能与兴趣爱好，通过地理位置发现附近的人，基于 **AI 语义匹配引擎** 进行智能匹配推荐，还可以在社区广场发布动态、添加好友、即时聊天。

### 核心亮点

- **AI 智能匹配** — 基于 TF-IDF 语义相似度 + 技能互补度 + 兴趣重叠度的多维匹配算法
- **向量缓存** — 用户画像向量化后存入 Redis，匹配时直接调用，毫秒级响应
- **地理位置发现** — 高德地图 API 定位，发现附近志同道合的人

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.2 | Web 框架 |
| Java | 21 | 编程语言 |
| MyBatis-Plus | 3.5.9 | ORM 框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.0 | 缓存 / Token 存储 / 向量缓存 |
| JWT (jjwt) | 0.13.0 | 用户认证 |
| 阿里云 OSS | 3.18.4 | 对象存储 |
| Hutool | 5.8.44 | Java 工具库 |

### AI 引擎

| 技术 | 版本 | 用途 |
|------|------|------|
| FastAPI | 0.115.0 | 高性能异步 Web 框架 |
| scikit-learn | 1.5.0 | TF-IDF 向量化 + 余弦相似度 |
| Redis | 5.0.0 | 向量缓存 |
| Pydantic | 2.8.0 | 数据校验 |
| Uvicorn | 0.30.0 | ASGI 服务器 |

### 用户端前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4 | 前端框架 |
| Vite | 5.0 | 构建工具 |
| Element Plus | 2.7 | UI 组件库 |
| Pinia | 2.1 | 状态管理 |
| Axios | 1.7 | HTTP 客户端 |

### 管理后台前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4 | 前端框架 |
| Vite | 5.4 | 构建工具 |
| PrimeVue | 4.5 | UI 组件库 |
| ECharts | 5.5 | 数据可视化图表 |
| XLSX | 0.18 | Excel 导出 |
| Pinia | 2.1 | 状态管理 |
| Axios | 1.7 | HTTP 客户端 |

## 功能模块

### 用户端

| 模块 | 核心功能 |
|------|---------|
| **用户系统** | 注册、登录、个人资料编辑、修改密码 |
| **技能标签** | 添加/管理个人技能，标签分类展示 |
| **兴趣爱好** | 添加/管理兴趣爱好标签 |
| **相册展示** | 图片上传至 OSS，个人相册管理 |
| **社区广场** | 发帖、评论、点赞、标签筛选 |
| **好友系统** | 好友申请、好友列表、好友管理 |
| **即时通讯** | 好友间一对一实时聊天 |
| **发现匹配** | AI 智能匹配 + 地理位置发现 |
| **通知系统** | 点赞通知、好友申请通知 |

### 管理后台

| 模块 | 核心功能 |
|------|---------|
| **数据仪表盘** | 用户增长趋势、活跃度统计、ECharts 可视化图表 |
| **用户管理** | 用户列表查询、状态启禁用、用户详情查看 |
| **标签管理** | 技能标签 / 兴趣标签的增删改查、分类管理 |
| **管理员管理** | 管理员账号的增删改查、权限控制 |

## AI 匹配引擎

### 匹配算法

匹配引擎采用 **三维加权融合** 算法，综合评估用户之间的匹配度：

```
最终得分 = TF-IDF 语义相似度 × 0.3 + 技能互补度 × 0.4 + 兴趣重叠度 × 0.3
```

| 维度 | 权重 | 计算方式 |
|------|------|---------|
| **TF-IDF 语义相似度** | 30% | 将用户简历、技能、爱好拼接为文本，TF-IDF 向量化后计算余弦相似度 |
| **技能互补度** | 40% | 我会的 ∩ 你想学的 + 你想学的 ∩ 我会的，归一化后得分 |
| **兴趣重叠度** | 30% | 双方兴趣标签的交集占比 |

### API 接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/ai/match` | POST | 批量语义匹配，返回候选用户得分排序 |
| `/api/ai/embed` | POST | 单用户文本转向量，写入 Redis 缓存 |
| `/api/ai/embed/batch` | POST | 批量更新用户向量 |
| `/api/ai/health` | GET | 健康检查 |

### 请求示例

```json
POST /api/ai/match
{
  "source": {
    "userId": "user_001",
    "bio": "全栈开发者，热爱开源",
    "canSkills": ["Java", "Vue", "MySQL"],
    "wantSkills": ["Python", "机器学习"],
    "hobbies": ["编程", "摄影", "篮球"]
  },
  "candidates": [
    {
      "userId": "user_002",
      "bio": "Python 工程师",
      "canSkills": ["Python", "TensorFlow"],
      "wantSkills": ["Java", "Vue"],
      "hobbies": ["机器学习", "篮球"]
    }
  ]
}
```

### 响应示例

```json
{
  "scores": [
    {
      "userId": "user_002",
      "tfidfSimilarity": 0.2145,
      "skillComplement": 0.6667,
      "interestOverlap": 0.3333,
      "score": 0.4110
    }
  ],
  "cost_ms": 12.35
}
```

### 向量缓存机制

```
用户资料更新 → 调用 /api/ai/embed → TF-IDF 向量化 → 写入 Redis (user:embed:{userId})
                                                        ↓
匹配请求 → 读取 Redis 缓存 → 直接用于相似度计算 → 毫秒级响应
```

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
├── ai-engine/              # AI 匹配引擎（FastAPI）
│   ├── app.py              # 主入口，定义 API 端点
│   ├── config.py           # 配置项（Redis、权重、端口）
│   ├── services/
│   │   ├── matcher.py      # 匹配算法（TF-IDF + 互补 + 重叠）
│   │   └── embedder_service.py  # 向量化服务 + Redis 缓存
│   ├── utils/
│   │   ├── text.py         # 文本构建工具
│   │   └── redis_client.py # Redis 客户端
│   ├── models/             # 数据模型
│   ├── tests/              # 单元测试
│   └── requirements.txt    # Python 依赖
├── frontend/               # 用户端前端（Vue 3 + Element Plus）
│   └── src/
│       ├── views/          # 页面组件（17 个页面）
│       ├── stores/         # Pinia 状态管理
│       ├── api/            # Axios 接口封装
│       ├── router/         # 路由配置（含导航守卫）
│       ├── components/     # 公共组件
│       └── utils/          # 工具函数
├── admin-frontend/         # 管理后台前端（Vue 3 + PrimeVue）
│   └── src/
│       ├── views/          # 页面组件（仪表盘、用户管理、标签管理等）
│       ├── stores/         # Pinia 状态管理
│       ├── api/            # Axios 接口封装
│       ├── router/         # 路由配置（含导航守卫）
│       └── utils/          # 工具函数
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
- **Python** 3.10+
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

### 4. 启动 AI 引擎

```bash
cd ai-engine
pip install -r requirements.txt
python app.py
# 或
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

AI 引擎默认运行在 `http://localhost:8000`。

### 5. 启动后端

```bash
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 6. 启动用户端前端

```bash
cd frontend
npm install
npm run dev
```

用户端前端默认运行在 `http://localhost:3000`，API 请求自动代理到后端。

### 7. 启动管理后台前端

```bash
cd admin-frontend
npm install
npm run dev
```

管理后台前端默认运行在 `http://localhost:3001`，API 请求自动代理到后端。

### 8. 访问应用

- 用户端：打开浏览器访问 `http://localhost:3000`
- 管理后台：打开浏览器访问 `http://localhost:3001`

## 架构说明

### 系统架构

```
┌─────────────┐     ┌─────────────┐
│ 用户端前端   │     │ 管理后台前端  │
│ Vue3 + EPlus│     │ Vue3 + PV   │
│  :3000      │     │  :3001      │
└──────┬──────┘     └──────┬──────┘
       │                   │
       └─────────┬─────────┘
                 │
          ┌──────▼──────┐
          │  后端 Spring │
          │   Boot 3.3  │
          │    :8080    │
          └──────┬──────┘
                 │
       ┌─────────┼─────────┐
       │                   │
┌──────▼──────┐     ┌──────▼──────┐
│  MySQL 8.0  │     │   Redis 7.0 │
│   数据库     │     │  缓存/向量   │
└─────────────┘     └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  AI 引擎    │
                    │  FastAPI    │
                    │   :8000    │
                    └─────────────┘
```

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

## 常用命令

```bash
# 后端
mvn compile          # 编译
mvn test             # 运行测试
mvn test -Dtest=Xxx  # 运行单个测试类
mvn package          # 打包
mvn clean            # 清理

# AI 引擎
cd ai-engine
python app.py                    # 启动服务
pytest tests/                    # 运行测试

# 用户端前端
cd frontend
npm run dev          # 开发模式
npm run build        # 生产构建
npm run preview      # 预览构建结果

# 管理后台前端
cd admin-frontend
npm run dev          # 开发模式
npm run build        # 生产构建
npm run preview      # 预览构建结果
```

## API 文档

- 后端接口文档：`docs/SkillMatch 接口文档.md`
- OpenAPI 规范：`docs/skillmatch-openapi.json`
- AI 引擎接口：启动后访问 `http://localhost:8000/docs`（Swagger UI）

## 服务端口汇总

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 Spring Boot | 8080 | REST API 服务 |
| AI 引擎 FastAPI | 8000 | 匹配算法服务 |
| 用户端前端 | 3000 | 用户端 Web 应用 |
| 管理后台前端 | 3001 | 管理后台 Web 应用 |

## 许可证

本项目仅供学习交流使用。

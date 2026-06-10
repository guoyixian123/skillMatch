# SkillMatch

> 基于技能标签与语义匹配的智能社交平台，帮助用户发现身边技能互补、兴趣相投的人。

## 项目简介

SkillMatch 是一个面向技能型社交的全栈 Web 应用。与传统"兴趣相同即推荐"的社交平台不同，SkillMatch 的核心理念是 **技能互补** —— 我会 Java，你想学 Java；你会 Python，我想学 Python —— 通过双向互补匹配，让社交关系产生真正的价值交换。

平台集成了 **SentenceTransformer 语义匹配引擎**，能够理解用户简介、技能和爱好的深层语义，实现"Java 开发 ≈ SpringBoot 工程师"级别的智能推荐，而非简单的关键词匹配。

### 核心亮点

- **技能互补推荐** — 不仅匹配兴趣相同的人，更发现技能互补的搭档
- **SentenceTransformer 语义匹配** — 基于预训练模型生成 384 维语义向量，通过余弦相似度计算匹配度
- **Redis GEO 空间召回** — 引入地理位置感知机制，提高匹配结果的现实可达性
- **多层匹配流水线** — 召回 → 过滤 → 粗排 → 精排，兼顾效率与精度
- **优雅降级** — AI 服务不可用时自动回退到规则匹配，保障系统可用性

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.3.2 | Web 框架 |
| MyBatis-Plus | 3.5.9 | ORM 框架（含分页插件） |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.0 | 缓存 / Token 存储 / GEO 空间索引 |
| JWT (jjwt) | 0.13.0 | 用户认证 |
| 阿里云 OSS | 3.18.4 | 对象存储（头像、图片） |
| Hutool | 5.8.44 | Java 工具库 |
| Lombok | 1.18.34 | 代码简化 |

### AI 引擎

| 技术 | 版本 | 用途 |
|------|------|------|
| FastAPI | 0.115.0 | 高性能异步 Web 框架 |
| SentenceTransformers | 3.0.0 | 预训练语义向量模型 |
| PyTorch | 2.0+ | 深度学习框架 |
| NumPy | 1.26.0 | 向量计算 |
| Redis | 5.0.0 | 向量缓存 |
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
| **技能标签** | 添加/管理"我会的"和"我想学的"技能，标签分类展示 |
| **兴趣爱好** | 添加/管理兴趣爱好标签 |
| **相册展示** | 图片上传至 OSS，个人相册管理 |
| **智能匹配** | 基于技能互补 + 语义相似度 + 地理位置的多维推荐 |
| **社区广场** | 发帖、评论、点赞、标签筛选 |
| **好友系统** | 技能交换请求、好友列表、好友管理 |
| **即时通讯** | 好友间一对一实时聊天 |
| **通知系统** | 点赞通知、交换请求通知 |

### 管理后台

| 模块 | 核心功能 |
|------|---------|
| **数据仪表盘** | 用户增长趋势、活跃度统计、ECharts 可视化图表 |
| **用户管理** | 用户列表查询（支持地理位置筛选）、状态启禁用 |
| **机器人管理** | 批量创建/删除测试用户 |
| **标签管理** | 技能标签 / 兴趣标签的增删改查、分类管理 |
| **管理员管理** | 管理员账号的增删改查、ROOT/ADMIN 权限控制 |

## 核心算法：多层匹配流水线

匹配引擎采用 **四阶段流水线** 架构，兼顾召回效率与匹配精度：

```
候选用户池
    │
    ▼
┌─────────────────────────────────┐
│  1. Geo 空间召回（Redis GEO）    │  ← 按距离筛选附近用户
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  2. 过滤层                       │  ← 排除自己、已有好友、待处理请求
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  3. 规则粗排                     │  ← 技能互补度 + 兴趣重叠度
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  4. AI 精排（SentenceTransformer）│  ← 语义向量余弦相似度
└──────────────┬──────────────────┘
               │
               ▼
          最终推荐结果
```

### 规则粗排：技能互补 + 兴趣重叠

```
ruleScore = skillComplement × 0.7 + hobbyOverlap × 0.3

skillComplement = (我会的 ∩ 你想学的 + 你想学的 ∩ 我会的) / 总技能数
hobbyOverlap = 双方兴趣标签交集 / 双方兴趣标签并集
```

### AI 精排：SentenceTransformer 语义匹配

传统 TF-IDF 基于关键词匹配，无法理解语义 —— "Java 开发"和"SpringBoot 工程师"在 TF-IDF 空间中几乎正交。

SentenceTransformer 将用户简介、技能、爱好拼接为文本，通过预训练模型编码为 **384 维语义向量**，再计算余弦相似度：

```
cos(θ) = A·B / (‖A‖ × ‖B‖)
```

在这个向量空间中，"Java 开发 ≈ SpringBoot 工程师"、"Python 开发 ≈ Django 开发"，能够真正理解上下文语义。

### 最终融合得分

```
finalScore = ruleScore × 0.6 + aiScore × 0.4
```

当候选人数不足 20 时，跳过 AI 精排，仅使用规则得分，降低延迟。

## 系统架构

```
┌─────────────┐     ┌─────────────┐
│  用户端前端   │     │  管理后台前端  │
│ Vue3 + EPlus│     │ Vue3 + PV   │
│   :3000     │     │   :3001     │
└──────┬──────┘     └──────┬──────┘
       │                   │
       └─────────┬─────────┘
                 │
          ┌──────▼──────┐
          │  Spring Boot │
          │    3.3.2    │
          │    :8080    │
          └──────┬──────┘
                 │
       ┌─────────┼─────────┐
       │                   │
┌──────▼──────┐     ┌──────▼──────┐
│  MySQL 8.0  │     │   Redis 7.0 │
│  15 张数据表 │     │ Token/GEO/缓存│
└─────────────┘     └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  AI 引擎    │
                    │  FastAPI    │
                    │SentenceTrans│
                    │   :8000    │
                    └─────────────┘
```

### 认证流程

```
登录/注册 → 生成 JWT Token → 存入 Redis → 返回前端
    ↓
后续请求携带 user_info 请求头
    ↓
TokenJWTInterceptor 拦截 → 解析 Token → Redis 校验有效性
    ↓
校验通过 → userId 写入 UserContext (ThreadLocal) → 业务处理
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
- 失败：`RESTful.error(code, message)` 或抛出 `BusinessException` → 全局异常处理器统一捕获

## 项目结构

```
skillMatch/
├── src/main/java/com/skillmatch/
│   ├── controller/         # REST 接口（16 个 Controller）
│   ├── service/            # 业务逻辑接口 + impl 实现
│   ├── mapper/             # MyBatis-Plus Mapper 接口
│   ├── domain/
│   │   ├── po/             # 持久化实体（15 张表）
│   │   ├── dto/            # 请求 DTO（20 个）
│   │   ├── vo/             # 响应 VO（25 个）
│   │   └── query/          # 分页/查询参数对象
│   ├── client/             # AI 引擎 HTTP 客户端
│   ├── config/             # Spring 配置（WebMvc、MyBatis-Plus、GeoSync）
│   ├── interceptor/        # JWT 认证拦截器
│   ├── context/            # UserContext（ThreadLocal）
│   ├── utils/              # 工具类（JwtUtil、OssUtil、GeoUtil）
│   ├── enums/              # ErrorCode 错误码枚举
│   ├── exceptions/         # BusinessException + GlobalExceptionHandler
│   ├── annotation/         # 自定义注解（@ValidTag）
│   └── validator/          # 自定义校验器
├── src/main/resources/
│   ├── mapper/             # MyBatis XML 映射文件（10 个）
│   └── application.yaml    # 主配置
├── ai-engine/              # AI 语义匹配引擎
│   ├── app.py              # FastAPI 主入口
│   ├── config.py           # 配置项（Redis、权重、模型路径）
│   ├── services/
│   │   ├── matcher.py      # 匹配算法（语义相似度 + 互补 + 重叠）
│   │   └── embedder_service.py  # SentenceTransformer 向量化 + Redis 缓存
│   ├── utils/
│   │   ├── text.py         # 文本构建工具
│   │   └── redis_client.py # Redis 客户端
│   └── requirements.txt    # Python 依赖
├── frontend/               # 用户端前端（Vue 3 + Element Plus）
│   └── src/
│       ├── views/          # 页面组件
│       ├── stores/         # Pinia 状态管理
│       ├── api/            # Axios 接口封装
│       └── router/         # 路由配置（含导航守卫）
├── admin-frontend/         # 管理后台前端（Vue 3 + PrimeVue）
│   └── src/
│       ├── views/          # 页面组件
│       ├── stores/         # Pinia 状态管理
│       ├── api/            # Axios 接口封装
│       └── router/         # 路由配置
├── docs/                   # 文档（接口文档、OpenAPI 规范）
├── test_data/              # 测试数据
└── init.sql                # 数据库初始化脚本
```

## 数据库设计

共 15 张表，使用 utf8mb4 字符集：

| 表名 | 说明 |
|------|------|
| `user` | 用户基础信息（账号、密码、昵称、头像、地理位置等） |
| `admin_user` | 管理员账号（ROOT/ADMIN 角色层级） |
| `skill_tag` | 技能标签字典（按分类排序） |
| `hobby_tag` | 兴趣标签字典（按分类排序） |
| `user_skill` | 用户技能关联（skillType: 1=我会的, 2=我想学的） |
| `user_hobby` | 用户兴趣爱好关联 |
| `user_gallery` | 用户相册图片 |
| `post` | 社区广场帖子 |
| `post_comment` | 帖子评论 |
| `post_tag` | 帖子标签关联 |
| `friend` | 好友关系 |
| `contact_request` | 技能交换请求（pending/accepted/declined） |
| `chat_message` | 聊天消息 |
| `like_info` | 点赞记录（用户点赞 / 帖子点赞） |
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

# 高德地图 API（地理编码）
export GAODE_API_KEY=your_gaode_key

# 阿里云 OSS（文件存储）
export OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
export OSS_ACCESS_KEY_ID=your_oss_ak
export OSS_ACCESS_KEY_SECRET=your_oss_sk
export OSS_BUCKET_NAME=your_bucket

# JWT 密钥
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

AI 引擎默认运行在 `http://localhost:8000`，首次启动会下载 SentenceTransformer 模型（约 90 MB）。

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

用户端默认运行在 `http://localhost:3000`。

### 7. 启动管理后台前端

```bash
cd admin-frontend
npm install
npm run dev
```

管理后台默认运行在 `http://localhost:3001`。

## 服务端口汇总

| 服务 | 端口 | 说明 |
|------|------|------|
| Spring Boot 后端 | 8080 | REST API 服务 |
| FastAPI AI 引擎 | 8000 | 语义匹配算法服务 |
| 用户端前端 | 3000 | 用户端 Web 应用 |
| 管理后台前端 | 3001 | 管理后台 Web 应用 |

## API 文档

- 后端接口文档：`docs/SkillMatch 接口文档.md`
- OpenAPI 规范：`docs/skillmatch-openapi.json`
- AI 引擎接口：启动后访问 `http://localhost:8000/docs`（Swagger UI）

## 常用命令

```bash
# 后端
mvn compile                    # 编译
mvn test                       # 运行测试
mvn package                    # 打包

# AI 引擎
cd ai-engine
python app.py                  # 启动服务
pytest tests/                  # 运行测试

# 前端（用户端 / 管理后台）
cd frontend  # 或 cd admin-frontend
npm run dev                    # 开发模式
npm run build                  # 生产构建
npm run preview                # 预览构建结果
```

## 许可证

本项目仅供学习交流使用。


#### SkillMatch 1.0.0 完工🥳

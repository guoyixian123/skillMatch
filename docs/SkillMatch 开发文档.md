# SkillMatch 开发文档

## 一、项目概述

SkillMatch 是一个基于 **Spring Boot + Python AI 推荐引擎** 协同驱动的轻社交技能匹配平台。用户发布"我会什么"和"想学什么"，系统通过 Redis GEO 地理围栏粗筛 + Java 技能互补度算法 + Python AI 精排三级管道，匹配附近最合适的技能交换伙伴，支持技能交换、找搭子、线下组局等场景。

**核心关键词**：技能互补匹配 · LBS 地理围栏 · AI 精排 · 轻社交 · 账号密码登录

---

## 二、技术选型

### 前端

| 技术 | 说明 |
| ---- | ---- |
| Vue 3 | 前端框架 |
| Vite | 构建工具 |
| Vue Router 4 | 路由管理 |
| Pinia | 状态管理 |
| pinia-plugin-persistedstate | Pinia 持久化插件 |
| Element Plus | UI 组件库 |
| Axios | HTTP 请求 |

### 后端

| 技术 | 说明 |
| ---- | ---- |
| Spring Boot 3.3.2 | 单体应用脚手架 |
| Spring MVC | Web 层 |
| MyBatis-Plus 3.5.x | ORM 持久层 |
| MySQL 8.0 | 关系型数据库 |
| Redis 6.2+ | 缓存 + GEO 地理索引 + Token 白名单 |
| Hutool | Java 工具库（Bean 拷贝、MD5 加密） |
| JWT (jjwt) | Token 签发与校验 |
| RabbitMQ | 异步消息队列（点赞计数、交换通知，初期用 @Async 替代） |

### AI 引擎（Python 独立模块）

| 技术 | 说明 |
| ---- | ---- |
| Flask / FastAPI | HTTP 服务暴露 |
| LambdaMART (LightGBM) | 学习排序（Learning to Rank）模型 |
| scikit-learn | 特征预处理与编码 |
| pandas / numpy | 数据处理 |

---

## 三、系统架构

```
┌──────────────────────────────────────────────┐
│              前端 (Vue + Element)              │
│              localhost:3000                   │
└──────────────────┬───────────────────────────┘
                   │ HTTP (Header: user_info=<JWT>)
┌──────────────────┴───────────────────────────┐
│         Spring Boot 单体应用 (skillMatch)       │
│              localhost:8080                   │
│                                                │
│  ┌──────────┐ ┌──────────┐ ┌───────────────┐  │
│  │ 匹配发现  │ │  社区模块  │ │  通知模块      │  │
│  │matching  │ │community │ │ notification  │  │
│  │ (待实现)  │ │  (已实现)  │ │  (已实现)      │  │
│  └──────────┘ └──────────┘ └───────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌───────────────┐  │
│  │ 用户模块  │ │  认证模块  │ │  通用模块      │  │
│  │  user    │ │   auth   │ │   common      │  │
│  │ (已实现)  │ │  (已实现)  │ │  (部分实现)    │  │
│  └──────────┘ └──────────┘ └───────────────┘  │
│                                                │
└────────┬──────────┬──────────┬────────────────┘
         │          │          │
    ┌────┴────┐ ┌───┴────┐ ┌──┴──────────┐
    │  MySQL  │ │ Redis  │ │  RabbitMQ   │
    │  :3306  │ │ :6379  │ │   :5672     │
    └─────────┘ └────────┘ └─────────────┘
         │
    ┌────┴──────────┐
    │ Alibaba Cloud │
    │     OSS       │
    └───────────────┘
         │
    ┌────┴──────────┐
    │ Python AI 引擎 │  ← HTTP 调用 (待实现)
    │ Flask :5000   │
    └───────────────┘
```

### 包结构

```
com.skillmatch
├── controller/
│   ├── AuthController.java           ✅ 已实现
│   ├── UserController.java           ✅ 已实现
│   ├── UserGalleryController.java    ✅ 已实现
│   ├── UserHobbyController.java      ✅ 已实现
│   ├── UserSkillController.java      ✅ 已实现
│   ├── PostController.java           ✅ 已实现
│   ├── PostCommentController.java    ✅ 已实现
│   ├── PostTagController.java        ⚠️ 空壳
│   ├── LikeInfoController.java       ✅ 已实现
│   ├── ContactRequestController.java ✅ 已实现
│   ├── MatchingController.java       ❌ 待实现
│   └── CommonController.java         ❌ 待实现
├── service/
│   ├── IAuthService.java / impl/             ✅
│   ├── IUserService.java / impl/             ✅
│   ├── IUserGalleryService.java / impl/      ✅
│   ├── IUserHobbyService.java / impl/        ✅
│   ├── IUserSkillService.java / impl/        ✅
│   ├── IPostService.java / impl/             ✅
│   ├── IPostCommentService.java / impl/      ✅
│   ├── IPostTagService.java / impl/          ⚠️ 空壳
│   ├── ILikeInfoService.java / impl/         ✅
│   ├── IContactRequestService.java / impl/   ✅
│   ├── IMatchingService.java / impl/         ❌ 待实现
│   ├── INotificationService.java / impl/     ❌ 待实现
│   └── ICommonService.java / impl/           ❌ 待实现
├── mapper/
│   ├── UserMapper.java               ✅
│   ├── UserSkillMapper.java          ✅
│   ├── UserHobbyMapper.java          ✅
│   ├── UserGalleryMapper.java        ✅
│   ├── PostMapper.java               ✅
│   ├── PostTagMapper.java            ✅
│   ├── PostCommentMapper.java        ✅
│   ├── LikeInfoMapper.java           ✅
│   ├── ContactRequestMapper.java     ✅
│   └── AuthMapper.java               ✅
├── domain/
│   ├── po/           # 持久化对象（数据库实体，使用 MyBatis-Plus 注解）
│   ├── dto/          # 数据传输对象（请求体）
│   ├── vo/           # 视图对象（响应体）
│   └── query/        # 查询参数对象
├── config/
│   ├── WebMvcConfig.java             # 注册 JWT 拦截器
│   └── MybatisPlusConfig.java        # MyBatis-Plus 分页插件
├── interceptor/
│   └── TokenJWTInterceptor.java      # JWT Token 校验拦截器
├── context/
│   └── UserContext.java              # ThreadLocal 用户 ID 持有者
├── enums/
│   └── ErrorCode.java                # 统一错误码枚举
├── exceptions/
│   ├── BusinessException.java        # 业务异常
│   └── GlobalExceptionHandler.java   # 全局异常处理器
├── utils/
│   ├── JwtUtil.java                  # JWT 工具（HMAC-SHA256）
│   └── OssUtil.java                  # 阿里云 OSS 上传/删除工具
├── constants/
│   └── RedisConstant.java            # Redis Key 常量
└── SkillMatchApplication.java        # 应用入口
```

---

## 四、数据库设计

### 4.1 用户表 `user`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| user_id | VARCHAR(32) PK | 登录账号（主键），4-16字符 |
| name | VARCHAR(32) NOT NULL | 昵称 |
| password | VARCHAR(128) NOT NULL | MD5 加密后的密码 |
| head_url | VARCHAR(255) | 头像 OSS URL（数据库字段名 head_url，PO 映射为 avatarUrl） |
| signature | VARCHAR(128) | 个性签名 |
| bio | TEXT | 个人简介 |
| contact_info | VARCHAR(64) | 联系方式（对方同意交换后可见） |
| latitude | DECIMAL(10,7) | 纬度，范围 -90~90 |
| longitude | DECIMAL(10,7) | 经度，范围 -180~180 |
| city | VARCHAR(50) | 城市名（高德逆地理编码获取，注册/更新位置时自动写入） |
| like_count | INT DEFAULT 0 | 获赞数（冗余） |
| post_count | INT DEFAULT 0 | 发帖数（冗余） |
| created_at | DATETIME DEFAULT NOW() | 注册时间 |
| updated_at | DATETIME DEFAULT NOW() ON UPDATE | 更新时间 |
| last_login_at | DATETIME | 最后登录时间 |

> 注意：当前 PO 类 `User` 中**没有** `cover_url` 和 `status` 字段。封面图功能待扩展。

### 4.2 技能标签表 `user_skill`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | VARCHAR(64) PK | 主键（雪花ID） |
| user_id | VARCHAR(32) NOT NULL | 用户 ID |
| skill_name | VARCHAR(32) NOT NULL | 技能名称 |
| skill_type | TINYINT NOT NULL | 1=我会 / 2=想学 |
| created_time | DATETIME DEFAULT NOW() | 创建时间 |

> 索引：`INDEX(user_id, skill_type)`。应用层限制每种类型最多 10 个。

### 4.3 兴趣爱好表 `user_hobby`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| user_id | VARCHAR(32) NOT NULL | 用户 ID |
| hobby_name | VARCHAR(32) NOT NULL | 爱好名称 |
| icon | VARCHAR(8) | emoji 图标 |
| created_at | DATETIME DEFAULT NOW() | 创建时间 |

### 4.4 图片墙表 `user_gallery`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| user_id | VARCHAR(32) NOT NULL | 用户 ID |
| image_url | VARCHAR(255) NOT NULL | OSS URL |
| sort_order | INT DEFAULT 0 | 排序 |
| created_at | DATETIME DEFAULT NOW() | 上传时间 |

### 4.5 动态表 `user_activity`（待实现）

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| user_id | VARCHAR(32) NOT NULL | 用户 ID |
| content | VARCHAR(255) NOT NULL | 动态文本 |
| activity_type | VARCHAR(16) NOT NULL | post / exchange / like / skill |
| related_id | VARCHAR(64) | 关联对象 ID |
| created_at | DATETIME DEFAULT NOW() | 发生时间 |

> 索引：`INDEX(user_id, created_at DESC)`

### 4.6 社区帖子表 `post`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| author_id | VARCHAR(32) NOT NULL | 作者 ID |
| title | VARCHAR(128) NOT NULL | 标题 |
| body | TEXT NOT NULL | 正文 |
| like_count | INT DEFAULT 0 | 点赞数（冗余） |
| comment_count | INT DEFAULT 0 | 评论数（冗余） |
| created_at | DATETIME DEFAULT NOW() | 发布时间 |
| updated_at | DATETIME | 编辑时间 |

> 索引：`INDEX(author_id)`、`INDEX(created_at DESC)`

### 4.7 帖子标签表 `post_tag`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| post_id | BIGINT NOT NULL | 帖子 ID |
| tag_name | VARCHAR(32) NOT NULL | 标签名（统一小写） |

> 索引：`INDEX(post_id)`、`INDEX(tag_name)`

### 4.8 通用点赞表 `like_info`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | VARCHAR(64) PK | 主键（雪花ID） |
| user_id | VARCHAR(32) NOT NULL | 点赞者 ID |
| biz_id | VARCHAR(32) NOT NULL | 业务对象 ID（用户ID 或 帖子ID） |
| type | INT NOT NULL | 点赞类型：1=主页点赞, 2=帖子点赞 |
| created_at | DATETIME DEFAULT NOW() | 点赞时间 |

> 唯一索引：`UNIQUE(user_id, biz_id, type)` — 同一人对同一对象同一类型只能点赞一次。

### 4.9 评论表 `post_comment`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| post_id | BIGINT NOT NULL | 帖子 ID |
| user_id | VARCHAR(32) NOT NULL | 评论者 ID |
| body | VARCHAR(500) NOT NULL | 评论内容 |
| created_at | DATETIME DEFAULT NOW() | 评论时间 |

> 索引：`INDEX(post_id, created_at)`

### 4.10 联系方式交换请求表 `contact_request`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | VARCHAR(64) PK | 主键（雪花ID） |
| from_user_id | VARCHAR(32) NOT NULL | 发起方 |
| to_user_id | VARCHAR(32) NOT NULL | 接收方 |
| reason | VARCHAR(200) NOT NULL | 请求理由 |
| status | INT NOT NULL DEFAULT 0 | 0=pending / 1=accepted / 2=declined |
| created_at | DATETIME DEFAULT NOW() | 发送时间 |
| updated_at | DATETIME | 处理时间 |

> 索引：`INDEX(from_user_id, status)`、`INDEX(to_user_id, status)`。应用层保证同一对用户间仅一个 pending 请求。

---

## 五、核心算法

### 5.1 匹配管道（三级漏斗）

```
┌─────────────────────────────────────────────────┐
│ 第一级：Redis GEO 粗筛                            │
│ GEOSEARCH user:locations FROMLONLAT {lng} {lat}  │
│   BYRADIUS {radius} km ASC WITHDIST COUNT 200    │
│ → 候选集 ≤ 200 人                                 │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────┴──────────────────────────────┐
│ 第二级：Java 技能互补度计算                        │
│ canMatch  = 对方想学 ∩ 我方会                     │
│ wantMatch = 对方会 ∩ 我方想学                     │
│ hobbyOverlap = 我方爱好 ∩ 对方爱好                │
│                                                  │
│ 互补分 = 0.4×|canMatch| + 0.4×|wantMatch|       │
│         + 0.2×|hobbyOverlap|                     │
│                                                  │
│ 按互补分降序排列                                   │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────┴──────────────────────────────┐
│ 第三级：Python AI 精排（候选 ≥ 20 人时触发）       │
│ LambdaMART 模型考虑特征：                         │
│  - 技能互补度分量                                  │
│  - 距离衰减因子                                    │
│  - 用户活跃度（lastLoginDays, postCount）          │
│  - 社交热度（likeCount）                           │
│  - 共同爱好数                                      │
│                                                  │
│ 输出：0~1 匹配置信度分数                           │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
              分页返回结果
```

**容错设计：**
- 用户未设置位置时，返回空列表并提示"请先设置位置"
- AI 引擎不可用时（超时 500ms 或返回错误），自动降级为纯 Java 规则排序
- `COUNT 200` 参数限制候选集上限，防止 OOM
- GEO 数据与 MySQL 双写，Redis 挂了可从 MySQL 恢复

### 5.2 GeoHash 编码

项目测试代码中包含 GeoHash 编码实现（`SkillMatchApplicationTests.java`），用于将经纬度编码为 52-bit 整型：

```java
// 26 bits 精度，经纬度位交织
long geoHash = encode(longitude, latitude);
```

> 当前实际匹配使用的是 Redis GEO（基于 ZSET + GeoHash），不直接使用此编码。此代码保留供后续离线计算或分桶查询使用。

### 5.3 JWT Token 设计

- **签发**：登录/注册成功时签发，有效期 7 天（可配置）
- **存储**：`{userId, issuedAt, expireAt}`
- **算法**：HMAC-SHA256 (HS256)
- **刷新**：旧 Token 保留 5 分钟宽限期（Redis 白名单过渡）
- **注销**：将当前 Token 加入 Redis 黑名单，TTL = Token 剩余有效期
- **校验**：`TokenJWTInterceptor` 从 Header `user_info` 读取 Token，验证是否在 Redis 白名单中

### 5.4 "最热"排序公式

```
score = (likeCount × 2 + commentCount × 3) / pow(hours + 2, 1.5)
```

> **注意**：当前 `PostServiceImpl.queryPostList()` 按 `likeCount` 直接排序，尚未实现此衰减公式。待完善。

### 5.5 AI 精排模型设计

**模型选型**：LambdaMART (LightGBM)

**选择理由**：
- LambdaMART 是 Learning to Rank 经典模型，直接优化 NDCG 排序指标
- LightGBM 训练速度快，支持大规模特征
- 相比协同过滤等方案，LambdaMART 不需要大量隐式反馈数据，冷启动友好

**特征设计**：

| 特征 | 类型 | 说明 |
| ---- | ---- | ---- |
| can_match_count | int | 对方想学 ∩ 我方会 |
| want_match_count | int | 对方会 ∩ 我方想学 |
| total_match_count | int | can + want |
| hobby_overlap | int | 共同爱好数 |
| distance_km | float | 距离 |
| distance_decay | float | 1 / (1 + distance_km) |
| like_count | int | 获赞数 |
| post_count | int | 发帖数 |
| last_login_days | int | 距上次登录天数（≤1=活跃，≤7=正常，>30=流失） |
| skill_density | float | 技能标签总数（衡量信息完整度） |
| has_gallery | bool | 是否有相册 |

**训练数据构建**：
- 正样本：双方有 accepted 交换请求
- 负样本：用户浏览但未发起交换请求（曝光未点击）
- 初期冷启动：使用规则打分结果 + 人工标注

**部署方式**：
- Python FastAPI + Uvicorn 独立部署在 `localhost:5000`
- Java 通过 `RestClient`（Spring 6.1+ 内置）同步调用
- 超时 500ms，失败自动降级

**Python 项目结构**（放在项目根目录 `ai-engine/`）：

```
ai-engine/
├── app.py                  # FastAPI 入口，路由定义
├── model/
│   └── lambdamart_v1.0.0.txt  # 训练好的 LightGBM 模型
├── schemas/
│   └── rank.py             # Pydantic 请求/响应模型
├── services/
│   └── rank_service.py     # 特征提取 + 模型推理逻辑
├── train/
│   ├── train.py            # 离线训练脚本
│   └── data.csv            # 训练数据（不提交 git）
├── requirements.txt
└── Dockerfile              # 部署用（可选）
```

**依赖**（`requirements.txt`）：

```
fastapi>=0.110.0
uvicorn[standard]>=0.27.0
pydantic>=2.0.0
lightgbm>=4.0.0
scikit-learn>=1.4.0
pandas>=2.0.0
numpy>=1.26.0
```

**启动方式**：

```bash
cd ai-engine
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 5000 --reload
```

> `--reload` 仅开发时使用，部署时去掉。启动后访问 `http://localhost:5000/docs` 可查看 Swagger UI。

**模型更新**：
- 离线训练，定期更新模型文件
- 通过 `/api/health` 接口暴露模型版本号
- 模型文件命名约定：`lambdamart_v{major}.{minor}.{patch}.txt`
- 初期每周更新一次，后续可接入自动化 pipeline

---

## 六、输入校验规范

| 字段 | 规则 | 位置 |
| ---- | ---- | ---- |
| userId | 4-16字符，字母/数字/下划线 | 注册 |
| password | 6-20字符，MD5 加密存储 | 注册/登录/修改密码 |
| name | 1-16字符 | 注册/编辑资料 |
| signature | ≤64字符 | 编辑资料 |
| bio | ≤500字符 | 编辑资料 |
| contactInfo | ≤64字符 | 编辑资料 |
| skillName | ≤20字符 | 技能标签 |
| hobbyName | ≤32字符 | 兴趣爱好 |
| 帖子 title | 1-64字符 | 发帖/编辑帖子 |
| 帖子 body | 1-5000字符 | 发帖/编辑帖子 |
| 帖子 tags | 每个≤16字符，≤5个，统一小写 | 发帖/编辑帖子 |
| 评论 body | 1-500字符 | 评论 |
| 交换理由 reason | 1-150字符 | 交换请求 |
| lat / lng | lat ∈ [-90, 90], lng ∈ [-180, 180] | 位置更新 |
| 文件上传 | 仅限 jpg/png/webp，≤10MB | 头像/相册/通用上传 |

> 所有文本写入数据库前做 HTML 转义（防 XSS）。

---

## 七、并发控制与容错

| 场景 | 方案 | 状态 |
| ---- | ---- | ---- |
| 主页点赞 | `UNIQUE(user_id, biz_id, type)` 约束，先查后插，重复返回 400 | ✅ |
| 帖子点赞 | 同上，type=2 | ✅ |
| 重复发起交换请求 | 应用层校验 + 同一对用户间只能有 1 个 pending | ✅ |
| accepted/declined 权限 | 仅接收方（toUserId == 当前用户）可操作 | ✅ |
| 发帖频率限制 | 应用层简单校验（待接入 Redis INCR + EXPIRE） | ⚠️ |
| 评论频率限制 | 同上 | ⚠️ |
| 位置更新 | `@Transactional` 保证 MySQL + Redis 双写一致性 | ✅ |
| AI 引擎降级 | 超时 500ms 或连接失败 → 降级为纯规则排序 | 设计阶段 |

---

## 八、消息队列设计

| 场景 | 交换机 | 路由键 | 消费者 |
| ---- | ---- | ---- | ---- |
| 点赞 | like.exchange | like.save | 异步更新对应业务的 like_count |
| 发帖成功 | community.exchange | post.create | 异步更新 user.post_count |
| 交换请求 | notification.exchange | request.send | 写入双方状态、生成动态 |

> **当前策略**：初期用同步数据库直写，流量增长后再接入 RabbitMQ。pom.xml 已包含 `spring-boot-starter-amqp` 依赖。

---

## 九、权限模型

### 9.1 访问控制矩阵

| 接口 | 未登录 | 已登录 | 交换已同意 |
| ---- | ---- | ---- | ---- |
| 发现页用户卡片 | ✅ | ✅ | ✅ |
| 用户名片弹窗 | ✅ (likeCount=0) | ✅ | ✅ |
| 用户完整主页 | ❌ | ❌（自己除外）| ✅ |
| 发起交换请求 | ❌ | ✅ | — |
| 我的主页（自己看自己） | ❌ | ✅ | — |
| 文件上传 | ❌ | ✅ | — |

### 9.2 "交换已同意"判定

```sql
SELECT 1 FROM contact_request
WHERE ((from_user_id = ? AND to_user_id = ?)
    OR (from_user_id = ? AND to_user_id = ?))
  AND status = 1  -- 1=accepted
LIMIT 1;
```

### 9.3 校验层级

Controller 层做第一道校验（`@Valid` / 参数判空），Service 层做第二道业务校验。全局异常处理器 `GlobalExceptionHandler` 统一返回 `{code, message, data}` 格式。

---

## 十、Redis Key 设计

| Key | 类型 | 说明 |
| ---- | ---- | ---- |
| `user:locations` | GEO | 用户经纬度索引 |
| `login:token:{userId}` | String | 登录 Token 白名单 |
| `blacklist:token:{token}` | String | 注销 Token 黑名单（TTL=剩余有效时间） |
| `user:like:{userId}` | Set | 用户点赞记录 |
| `like:count:{type}:{bizId}` | String | 点赞计数缓存 |

---

## 十一、部署清单

| 组件 | 端口 | 说明 |
| ---- | ---- | ---- |
| Spring Boot | 8080 | 主应用 |
| Python AI 引擎 (FastAPI + Uvicorn) | 5000 | LambdaMART 精排 HTTP 服务（待实现） |
| MySQL | 3306 | 关系型数据库 `skill_match` |
| Redis | 6379 | 缓存 + GEO + Token 管理 |
| RabbitMQ | 5672 | 消息队列（可选，初期用同步操作） |
| OSS | — | Alibaba Cloud OSS 图片存储 |
| Vue Dev Server | 3000 | 前端开发服务器（Vite） |

### 环境变量 / 配置要点

```yaml
# application.yaml 关键配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/skill_match
    username: root
    password: g091600jiof
  servlet:
    multipart:
      max-file-size: 10MB

# 高德地图配置（待添加）
amap:
  key: your-amap-web-api-key

# AI 引擎配置（待添加）
ai:
  matching:
    enabled: true
    base-url: http://localhost:5000
    timeout-ms: 500
    min-candidates: 20      # 候选数 ≥ 此值才调用 AI
```

---

## 十二、后端实现建议

> 根据 CLAUDE.md 约束，以下为建议而非直接修改。

### 12.1 MatchingController 实现建议

```
GET  /api/matching/users              → IMatchingService.getRecommendedUsers()
GET  /api/matching/users/{userId}/card → IMatchingService.getUserCard()
GET  /api/matching/users/{userId}/profile → IMatchingService.getUserProfile()
```

- `getRecommendedUsers()` 需要先查当前用户的位置和技能，若位置为空直接返回空列表
- 使用 `redisTemplate.opsForGeo().search()` 做 GEO 粗筛
- 排除本人和已有 pending 请求的用户
- 候选 ≥ `ai.matching.min-candidates` 时异步调用 AI 引擎精排

### 12.2 IMatchingService 核心逻辑

```java
public PageVO<UserCardVO> getRecommendedUsers(MatchingQuery query) {
    // 1. 获取当前用户位置，为空则返回空列表
    // 2. Redis GEOSEARCH 获取附近用户（COUNT 200）
    // 3. 过滤：排除本人 + 已有 pending 关系的用户
    // 4. 加载候选用户的技能标签和爱好
    // 5. 逐人计算互补度分数
    // 6. 按 sort 参数排序（score/dist/active）
    // 7. 若候选 >= 20，调用 AI 精排（try-catch 降级）
    // 8. 分页返回
}
```

### 12.3 CommonController 实现建议

```
POST /api/common/upload → OssUtil.upload()
```

封装已有的 `OssUtil` 为 REST 接口，增加格式和大小校验。

### 12.4 现有代码完善建议

| 优先级 | 事项 | 说明 |
| ---- | ---- | ---- |
| 高 | PostVO 补充 title 字段 | 帖子详情接口缺少标题 |
| 高 | 最热排序实现 HN 衰减公式 | 当前直接按 likeCount 排序 |
| 中 | 用户动态接入 | `user_activity` 表已设计，发帖/点赞/交换时写入 |
| 中 | 密码加密升级 | 当前使用 MD5，建议迁移到 BCrypt |
| 中 | Token 刷新/黑名单完善 | AuthController 的 refresh 和 logout 逻辑待完善 |
| 低 | User PO 增加 cover_url | 支持封面图功能 |
| 低 | User PO 增加 status 字段 | 支持用户状态管理（正常/禁言/封禁） |

### 12.5 AI 引擎实现建议

#### Python 侧

**`ai-engine/app.py`** — 只做路由，不写业务逻辑：

```python
from fastapi import FastAPI
from schemas.rank import RankRequest, RankResponse
from services.rank_service import rank_service

app = FastAPI(title="SkillMatch AI Engine", version="1.0.0")

@app.post("/api/rank", response_model=RankResponse)
def rank(req: RankRequest):
    return rank_service.rank(req.userId, req.candidates)

@app.get("/api/health")
def health():
    return {"status": "ok", "model_version": rank_service.model_version}
```

**`ai-engine/schemas/rank.py`** — Pydantic 模型，字段与 Java 侧 `AiRankRequest` 对齐：

```python
from pydantic import BaseModel

class Candidate(BaseModel):
    userId: str
    canMatchCount: int
    wantMatchCount: int
    hobbyOverlapCount: int
    distanceKm: float
    likeCount: int
    postCount: int
    lastLoginDays: int

class RankRequest(BaseModel):
    userId: str
    candidates: list[Candidate]

class RankResult(BaseModel):
    userId: str
    score: float

class RankResponse(BaseModel):
    code: int = 200
    data: list[RankResult]
```

**`ai-engine/services/rank_service.py`** — 模型加载 + 推理：

```python
import lightgbm as lgb
import os

class RankService:
    def __init__(self):
        model_dir = os.path.dirname(os.path.dirname(__file__))
        self.model_version = "1.0.0"
        self.model = lgb.Booster(
            model_file=os.path.join(model_dir, f"model/lambdamart_v{self.model_version}.txt")
        )

    def rank(self, userId: str, candidates: list) -> dict:
        # 构造特征矩阵 → model.predict() → 按分数降序排序
        # ...
        return {"code": 200, "data": [...]}

rank_service = RankService()  # 模块级单例，启动时加载模型
```

#### Java 侧（调用 Python）

**`AppConfig.java`** — 创建 RestClient Bean（Spring Boot 3.2+ 内置，无需额外依赖）：

```java
@Configuration
public class AppConfig {
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
```

**`MatchingServiceImpl.java`** — 调用 AI 精排，超时降级：

```java
@Value("${ai.matching.base-url:http://localhost:5000}")
private String aiBaseUrl;

public List<RankResult> callAiRank(AiRankRequest request) {
    try {
        return restClient.post()
                .uri(aiBaseUrl + "/api/rank")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<RankResult>>() {});
    } catch (Exception e) {
        log.warn("AI 引擎不可用，降级为规则排序", e);
        return Collections.emptyList();
    }
}
```

> `RestClient` 是 Spring 6.1 引入的同步 HTTP 客户端，已包含在 `spring-boot-starter-web` 中，无需添加 `spring-boot-starter-webflux`。

---

## 十三、前端页面路由

| 路由 | 视图组件 | 说明 |
| ---- | ---- | ---- |
| `/login` | LoginView | 登录页 |
| `/register` | RegisterView | 注册页 |
| `/discover` | DiscoverView | 发现页（匹配推荐） |
| `/community` | CommunityView | 社区帖子列表 |
| `/community/post/:id` | PostDetailView | 帖子详情 |
| `/notifications` | NotificationsView | 通知（交换请求） |
| `/profile` | MyProfileView | 我的主页 |
| `/profile/edit` | EditProfileView | 编辑资料 |
| `/profile/skills` | SkillManagerView | 技能管理 |
| `/profile/hobbies` | HobbyManagerView | 爱好管理 |
| `/profile/gallery` | GalleryManagerView | 相册管理 |
| `/profile/password` | ChangePasswordView | 修改密码 |
| `/user/:userId` | UserProfileView | 他人主页 |
| — | UserCardPopover | 用户名片弹窗（全局复用组件） |

---

## 十四、用户名片功能设计

### 14.1 设计目标

在任意显示用户头像的位置（发现页、社区帖子、评论列表、通知列表），**点击头像**弹出轻量级"用户名片" Popover，展示该用户的核心公开信息。用户无需跳转页面即可快速了解对方，降低社交摩擦。

**三个层次的信息查看体系**：

```
头像点击 → 名片 Popover（轻量，不离开当前页面）
         ↓ "查看主页"
       用户完整主页（重量，页面跳转）
```

### 14.2 前端组件设计

#### 14.2.1 组件：`UserCardPopover.vue`

位置：`frontend/src/components/UserCardPopover.vue`

**Props**

| Prop | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| userId | String | 是 | 目标用户 ID |
| trigger | String | 否 | 触发方式：`click`（默认）/ `hover` |
| placement | String | 否 | 弹出位置：`bottom`（默认）/ `top` / `left` / `right` |
| showActions | Boolean | 否 | 是否显示操作按钮，默认 true |

**Events**

| Event | 参数 | 说明 |
| ---- | ---- | ---- |
| view-profile | userId | 用户点击"查看主页" |
| send-request | userId | 用户点击"发起交换" |

**使用方式**

```html
<!-- 在任意头像上使用 -->
<UserCardPopover :userId="author.userId" placement="bottom">
  <img :src="author.avatarUrl" class="avatar" />
</UserCardPopover>
```

组件内部使用 Vue 的 `<slot>` 接收触发元素（头像），`<Teleport>` + 绝对定位实现 Popover。

#### 14.2.2 名片布局

```
┌──────────────────────────────────┐
│  ┌────┐                          │
│  │头像 │  用户名                  │
│  │64px│  "签名内容..."           │
│  └────┘  ❤128  📝5              │
│──────────────────────────────────│
│  我会: [Python] [Flask] [SQL]    │
│  想学: [吉他] [摄影]             │
│──────────────────────────────────│
│  [💬 发起交换]    [查看主页 →]   │
└──────────────────────────────────┘
```

> **设计原则**：紧凑优先。技能标签每类最多显示 5 个，超出显示 `+N`。

#### 14.2.3 数据获取

组件 `onMounted` 时调用 `getUserCard(userId)`：

```javascript
// frontend/src/api/user.js 新增
export function getUserCard(userId) {
  return request.get(`/user/card/${userId}`)
}
```

**缓存策略**：组件内部用 `shallowRef` + `Map` 缓存已加载的名片数据，同一 userId 不重复请求。

#### 14.2.4 触发场景汇总

| 页面/组件 | 头像位置 | 触发方式 | 备注 |
| ---- | ---- | ---- | ---- |
| DiscoverView 用户卡片 | `.card-avatar` | click | 拦截 `@click.stop` 防止冒泡到卡片 |
| CommunityView 帖子列表 | `.brutal-avatar` (作者头像) | click | 替换当前无行为状态 |
| PostDetailView 帖子详情 | 作者头像 + 评论者头像 | click | — |
| NotificationsView 通知列表 | 发起者头像 | click | — |
| UserProfileView 主页 | 已经是完整主页 | 不需要 | — |

### 14.3 后端 API 设计

#### 14.3.1 新增接口

```
GET /api/user/card/{userId}
```

> 无需登录。详见接口文档 2.0 节。

#### 14.3.2 新增 VO：`UserCardVO`

```java
package com.skillmatch.domain.vo;

@Data
public class UserCardVO {
    private String userId;
    private String name;
    private String avatarUrl;
    private String signature;
    private Integer likeCount;
    private Integer postCount;
    private List<String> canSkills;   // 我会的（最多5个）
    private List<String> wantSkills;  // 想学的（最多5个）
    private Boolean hasPendingRequest; // 当前用户是否有pending请求
}
```

> **注意**：`hasPendingRequest` 为 null（未登录）或 boolean。不包含 `bio`、`contactInfo` 等隐私字段。

#### 14.3.3 后端实现建议

**UserMapper 新增方法**：

```java
// 联表查询用户基本信息 + 技能标签
@Select("SELECT u.user_id, u.name, u.head_url AS avatarUrl, u.signature, " +
        "u.like_count AS likeCount, u.post_count AS postCount " +
        "FROM user u WHERE u.user_id = #{userId}")
UserCardVO selectUserCard(@Param("userId") String userId);

// 查询用户技能（我会的）
@Select("SELECT skill_name FROM user_skill WHERE user_id = #{userId} AND skill_type = 1 LIMIT 5")
List<String> selectCanSkills(@Param("userId") String userId);

// 查询用户技能（想学的）  
@Select("SELECT skill_name FROM user_skill WHERE user_id = #{userId} AND skill_type = 2 LIMIT 5")
List<String> selectWantSkills(@Param("userId") String userId);
```

**UserServiceImpl 新增方法**：

```java
public UserCardVO getUserCard(String userId) {
    UserCardVO card = userMapper.selectUserCard(userId);
    if (card == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    card.setCanSkills(userMapper.selectCanSkills(userId));
    card.setWantSkills(userMapper.selectWantSkills(userId));
    // hasPendingRequest：已登录用户查询是否已有 pending 请求
    String currentUserId = UserContext.getUserId();
    if (currentUserId != null) {
        card.setHasPendingRequest(
            contactRequestMapper.existsPending(currentUserId, userId)
        );
    }
    return card;
}
```

**UserController 新增方法**：

```java
@GetMapping("/card/{userId}")
public RESTful<UserCardVO> getUserCard(@PathVariable String userId) {
    return RESTful.ok(userService.getUserCard(userId));
}
```

### 14.4 实施阶段

**Phase 1 — 后端接口（优先）**

1. 创建 `UserCardVO`（`domain/vo/` 下）
2. `UserMapper` 添加 3 个查询方法
3. `UserServiceImpl` 添加 `getUserCard()` 方法
4. `UserController` 暴露 `GET /api/user/card/{userId}`

> 注意：`ContactRequestMapper` 需新增 `existsPending(fromUserId, toUserId)` 查询方法。

**Phase 2 — 前端组件与 API**

1. `frontend/src/api/user.js` 新增 `getUserCard(userId)` 方法
2. 创建 `frontend/src/components/UserCardPopover.vue`
3. 在 DiscoverView 中替换内联 `el-dialog` 为 `UserCardPopover`
4. 在 CommunityView、PostDetailView、NotificationsView 头像上绑定组件

**Phase 3 — 匹配扩展（可选）**

1. 实现 `GET /api/matching/users/{userId}/card`，返回 `UserCardVO` + `distance` + `matchScore`
2. 实现 `GET /api/matching/users` 真实推荐逻辑
3. 关闭 `matching.js` 中的 `USE_MOCK`

### 14.5 现有代码改进建议

| 优先级 | 事项 | 说明 |
| ---- | ---- | ---- |
| **高** | UserVO 去掉 password 字段 | `UserVO.java:13` 包含 `private String password`，查询他人信息时不应暴露 |
| **高** | 实现 `GET /api/user/card/{userId}` | 名片功能的唯一后端依赖 |
| 中 | `GET /api/user/skills` 增加 userId 参数 | 当前仅支持查自己的技能，无法查他人技能 |
| 中 | User PO 增加 `cover_url` 字段 | 个人主页封面图 |
| 低 | 密码加密升级 MD5 → BCrypt | 安全性增强

### 12.6 逆地理编码实现建议

**功能**：用户更新位置时，调用高德逆地理编码 API，将经纬度转为城市名存储。

**调用时机**：`UserServiceImpl.updateLocation()` 存完经纬度后，异步调用逆地理编码接口，不阻塞主流程。

**高德 API**：

```
GET https://restapi.amap.com/v3/geocode/regeo
  ?location={lng},{lat}
  &key={amapKey}
  &output=JSON
```

**响应取值路径**：`regeocode.addressComponent.city`。若 city 为空数组 `[]`，降级取 `district`，再为空取 `province`。

**容错**：
- API 调用失败 → 不抛异常，city 保持原值，仅 log.warn
- key 未配置（`${amap.key:}` 为空）→ 跳过逆地理编码
- 超时 3s → 降级

**UserServiceImpl.updateLocation() 改动**：

```java
@Value("${amap.key:}")
private String amapKey;

// 在 updateLocation() 末尾追加：
if (amapKey != null && !amapKey.isBlank()) {
    String city = resolveCity(location.getLongitude(), location.getLatitude());
    if (city != null) {
        lambdaUpdate()
            .eq(User::getUserId, userId)
            .set(User::getCity, city)
            .update();
    }
}

private String resolveCity(double lng, double lat) {
    try {
        String url = "https://restapi.amap.com/v3/geocode/regeo" +
            "?location=" + lng + "," + lat + "&key=" + amapKey + "&output=JSON";
        // 用 RestTemplate 请求，取出 regeocode.addressComponent.city
        // city 为 [] 时降级取 district → province
    } catch (Exception e) {
        log.warn("逆地理编码失败, lng={}, lat={}", lng, lat, e);
        return null;
    }
}
```

**需要新增/修改的文件**：

| 文件 | 改动 |
| ---- | ---- |
| `User.java` | 新增 `city` 字段 |
| `UserCardVO.java` | 新增 `city` 字段 |
| `UserServiceImpl.java` | `updateLocation()` 末尾加逆地理编码调用 |
| `MatchingServiceImpl.java` | `getRecommendedUsers()` 组装 city 到卡片 |
| `application.yaml` | 新增 `amap.key` 配置 |
| `user` 表 | `ALTER TABLE user ADD COLUMN city VARCHAR(50)` |

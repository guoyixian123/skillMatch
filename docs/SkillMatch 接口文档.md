# SkillMatch 接口文档

## 通用约定

### 基础路径

```
http://localhost:8080/api
```

> 前端开发通过 `http://localhost:3000` 访问，Vite 自动代理 `/api` 请求到后端 8080 端口。

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| code | 含义 |
| ---- | -------------------- |
| 200  | 成功 |
| 400  | 参数错误 |
| 401  | 未登录（Token 缺失或过期） |
| 403  | 无权限（如未交换同意就想看对方完整主页） |
| 404  | 资源不存在 |
| 429  | 频率限制（点赞超限、发帖超限等） |
| 500  | 服务端异常 |

### 认证方式

Header 携带 Token（登录后获取，有效期 7 天）：

```
user_info: <token>
```

登录校验由 Spring MVC 拦截器统一处理。标注了 **"需要登录"** 的接口必须携带有效 Token，否则返回 401。

---

## 一、认证模块 `/api/auth`

### 1.1 用户注册

> 无需登录

```
POST /api/auth/register
```

**请求体**

```json
{
  "userId": "4242428",
  "name": "张同学",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | ------------------- |
| userId | string | 是 | 账号，4-16字符，字母/数字/下划线 |
| name | string | 是 | 昵称，1-16字符 |
| password | string | 是 | 密码，6-20字符 |

**响应**

```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "userId": "37498472932",
      "name": "张同学",
      "avatarUrl": null
    }
  }
}
```

> 注册成功后自动登录，返回 JWT Token。userId 唯一，重复注册返回 400 "账号已被注册"。

### 1.2 用户登录

> 无需登录

```
POST /api/auth/login
```

**请求体**

```json
{
  "userId": "zhangsan",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | -- |
| userId | string | 是 | 账号 |
| password | string | 是 | 密码 |

**响应**

```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "userId": "zhangsan",
      "name": "张同学",
      "avatarUrl": "https://oss.xxx.com/avatar/1.jpg"
    }
  }
}
```

> 账号或密码错误返回 400 "账号或密码错误"。

### 1.3 刷新 Token

> 需要登录

```
POST /api/auth/refresh
```

**响应**

```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

> 返回新 Token，旧 Token 保留 5 分钟宽限期，避免并发请求失败。

### 1.4 退出登录

> 需要登录

```
POST /api/auth/logout
```

**响应**

```json
{
  "code": 200,
  "message": "已退出登录",
  "data": null
}
```

> 将当前 Token 加入 Redis 黑名单（TTL = Token 剩余有效期）。

---

## 二、用户模块 `/api/user`

### 2.0 获取用户名片（弹窗）

> 无需登录即可访问（未登录时 `likeCount` 不统计、`hasPendingRequest` 固定为 false）。

```
GET /api/user/card/{userId}
```

**响应**

```json
{
  "code": 200,
  "data": {
    "userId": "zhangsan",
    "name": "张同学",
    "avatarUrl": "https://oss.xxx.com/avatar/1.jpg",
    "signature": "代码改变世界",
    "likeCount": 128,
    "postCount": 5,
    "canSkills": ["Python", "Flask", "SQL"],
    "wantSkills": ["吉他", "摄影"],
    "hasPendingRequest": false
  }
}
```

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| userId | string | 用户 ID |
| name | string | 昵称 |
| avatarUrl | string | 头像 URL |
| signature | string | 个性签名 |
| likeCount | int | 获赞数（未登录时为 0） |
| postCount | int | 发帖数 |
| canSkills | array | 我会的技能名称列表，最多 5 个 |
| wantSkills | array | 想学的技能名称列表，最多 5 个 |
| hasPendingRequest | boolean | 当前登录用户是否已向该用户发送 pending 交换请求（未登录时固定 false） |

> **使用场景**：点击任意位置的用户头像时，弹出名片弹窗展示核心信息。与完整主页（2.1）的区别：
> - 不返回 `bio`、`contactInfo` 等隐私字段
> - 技能仅返回名称列表（最多 5 个），不包含技能 ID
> - 一次查询完成，无需前端多次请求

> **后端实现建议**：单个 SQL 联表查询 `user` + `user_skill`，应用层按 `skill_type` 分组。不查 `user_hobby`、`user_gallery` 等无关表。SQL 参考：
> ```sql
> SELECT u.user_id, u.name, u.head_url, u.signature,
>        u.like_count, u.post_count,
>        s.skill_name, s.skill_type
> FROM user u
> LEFT JOIN user_skill s ON u.user_id = s.user_id
> WHERE u.user_id = #{userId}
> ```

### 2.1 获取用户主页

> 需要登录

```
GET /api/user/profile/{userId}
```

**响应**

```json
{
  "code": 200,
  "data": {
    "userId": "zhangsan",
    "name": "张同学",
    "avatarUrl": "https://oss.xxx.com/avatar/1.jpg",
    "signature": "代码改变世界",
    "bio": "热爱编程和音乐，Python写了三年...",
    "contactInfo": "微信号：chen_xm2024",
    "likeCount": 128,
    "postCount": 5
  }
}
```

> `likeCount` 和 `postCount` 待后端完善。

### 2.2 更新个人资料

> 需要登录

```
PUT /api/user/profile
```

**请求体**

```json
{
  "name": "陈小明",
  "signature": "代码和吉他，一个都不能少",
  "bio": "热爱编程和音乐，Python写了三年...",
  "contactInfo": "微信号：chen_xm2024"
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | --------------- |
| name | string | 否 | 昵称，1-16字符，不传不更新 |
| signature | string | 否 | 个性签名，≤64字符 |
| bio | string | 否 | 个人简介，≤500字符 |
| contactInfo | string | 否 | 联系方式，≤64字符 |

**响应**

```json
{
  "code": 200,
  "message": "资料已经更新",
  "data": null
}
```

### 2.3 更新头像

> 需要登录

```
PUT /api/user/avatar
```

Content-Type: `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | -------------------------- |
| avatarUrl | File | 是 | 图片文件，仅限 jpg/png/webp，≤10MB |

> 上传新头像时自动删除 OSS 上的旧头像。

### 2.4 修改密码

> 需要登录

```
PUT /api/user/password
```

**请求体**

```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | ---------- |
| oldPassword | string | 是 | 当前密码 |
| newPassword | string | 是 | 新密码，6-20字符 |

> 旧密码错误返回 400 "旧密码错误"。

### 2.5 注销账号

> 需要登录

```
DELETE /api/user/account
```

**响应**

```json
{
  "code": 200,
  "message": "账号已注销",
  "data": null
}
```

> **待实现**：注销后用户数据不可恢复，Token 立即失效。

### 2.6 获取我的技能标签

> 需要登录

```
GET /api/user/skills
```

**响应**

```json
{
  "code": 200,
  "data": {
    "canSkills": [
      {"id": "1", "skillName": "Python"},
      {"id": "2", "skillName": "Flask"}
    ],
    "wantSkills": [
      {"id": "3", "skillName": "吉他"},
      {"id": "4", "skillName": "摄影"}
    ]
  }
}
```

### 2.7 添加技能标签

> 需要登录

```
POST /api/user/skills
```

**请求体**

```json
{
  "skillName": "Python",
  "skillType": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | ------------ |
| skillName | string | 是 | 技能名称，≤20字符 |
| skillType | int | 是 | 1=我会的 / 2=我想学的 |

> 每种类型最多 10 个，超限返回 400 "技能标签已达上限（10个）"。

### 2.8 删除技能标签

> 需要登录

```
DELETE /api/user/skills/{skillId}
```

> 校验：只能删除自己的标签，不存在或无权返回 404/403。

### 2.9 批量设置技能

> 需要登录

```
PUT /api/user/skills
```

**请求体**

```json
{
  "canSkills": ["Python", "Flask", "SQL"],
  "wantSkills": ["吉他", "摄影", "日语"]
}
```

> 全量替换：先删后插。`canSkills` 最多 20 个，`wantSkills` 最多 20 个。

### 2.10 获取我的兴趣爱好

> 需要登录

```
GET /api/user/hobbies
```

**响应**

```json
{
  "code": 200,
  "data": {
    "list": [
      {"id": "1", "hobbyName": "篮球", "icon": "🏀"},
      {"id": "2", "hobbyName": "吉他", "icon": "🎸"}
    ]
  }
}
```

### 2.11 添加兴趣爱好

> 需要登录

```
POST /api/user/hobbies
```

**请求体**

```json
{
  "hobbyName": "篮球",
  "icon": "🏀"
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | ---------- |
| hobbyName | string | 是 | 爱好名称，≤32字符 |
| icon | string | 否 | emoji 图标 |

### 2.12 删除兴趣爱好

> 需要登录

```
DELETE /api/user/hobbies/{hobbyId}
```

### 2.13 获取我的相册

> 需要登录

```
GET /api/user/gallery
```

**响应**

```json
{
  "code": 200,
  "data": {
    "list": [
      {"id": "1", "imageUrl": "https://oss.xxx.com/gallery/1.jpg", "sortOrder": 0, "createdAt": "2026-05-12T14:30:00"}
    ]
  }
}
```

### 2.14 上传图片到相册

> 需要登录

```
POST /api/user/gallery
```

Content-Type: `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | -------------------------- |
| file | File | 是 | 图片文件，仅限 jpg/png/webp，≤10MB |

**响应**

```json
{
  "code": 200,
  "data": {
    "id": "1",
    "imageUrl": "https://oss.xxx.com/gallery/1.jpg",
    "sortOrder": 0
  }
}
```

### 2.15 删除相册图片

> 需要登录

```
DELETE /api/user/gallery/{imageId}
```

### 2.16 更新位置

> 需要登录

```
PUT /api/user/location
```

**请求体**

```json
{
  "latitude": 39.9042,
  "longitude": 116.4074
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | -------------- |
| latitude | double | 是 | 纬度，范围 -90~90 |
| longitude | double | 是 | 经度，范围 -180~180 |

> 写入 Redis GEO 索引供匹配服务使用，同时更新 MySQL 中的经纬度字段。更新位置时自动调用高德逆地理编码 API，将城市名写入 `city` 字段。若高德 API 不可用或 key 未配置，city 字段保持原值，不影响定位更新。

### 2.17 点赞

> 需要登录。通用点赞接口，支持主页点赞和帖子点赞。

```
POST /api/like
```

**请求体**

```json
{
  "bizId": "2",
  "type": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | --------- |
| bizId | string | 是 | 业务对象 ID（用户ID 或 帖子ID） |
| type | int | 是 | 1=主页点赞 / 2=帖子点赞 |

**响应**

```json
{
  "code": 200,
  "message": "点赞成功",
  "data": {
    "likeCount": 129
  }
}
```

> 不能给自己点赞，不能重复点赞同一对象。重复点赞返回 400。

### 2.18 我的动态

> 需要登录

```
GET /api/user/activity?page=1&size=10
```

**响应**

```json
{
  "code": 200,
  "data": {
    "total": 25,
    "page": 1,
    "size": 10,
    "list": [
      {
        "id": "1",
        "type": "post",
        "content": "发布了帖子《找Python搭子一起刷LeetCode》",
        "relatedId": "1",
        "createdAt": "2026-05-12T14:30:00"
      }
    ]
  }
}
```

> **待实现**：用户动态表 `user_activity` 已设计但后端尚未接入。

---

## 三、匹配发现模块 `/api/matching`

> **状态**：后端 MatchingController 和 IMatchingService **待实现**。以下为完整接口设计。

匹配发现模块是 SkillMatch 的核心功能，结合 **Redis GEO 地理围栏 + Java 技能互补度算法 + Python AI 精排引擎** 三级管道，为用户推荐附近最适合的技能交换伙伴。

### 3.1 AI 匹配管道架构

```
请求 → Redis GEO 粗筛(≤200人)
     → Java 技能互补度计算(排序)
     → 候选≥20人时调用 Python AI 精排
     → 分页返回
```

### 3.2 获取推荐用户列表

> 需要登录（需要当前用户的技能数据做互补度计算）

```
GET /api/matching/users
```

| 参数 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | -------------------------------------------------- |
| filter | string | 否 | all=全部 / exchange=技能交换 / partner=找搭子 / active=最近活跃 |
| sort | string | 否 | score=匹配度（默认） / dist=距离 / active=活跃度 |
| keyword | string | 否 | 搜索关键词（昵称、简介、技能名称） |
| radius | int | 否 | 搜索半径（km），默认 5，最大 50 |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 12，最大 50 |

> 结果自动排除：1) 本人 2) 已有 pending 交换请求的用户。用户未设置位置时返回空列表并提示"请先设置位置"。

**响应**

```json
{
  "code": 200,
  "data": {
    "total": 42,
    "page": 1,
    "size": 12,
    "list": [
      {
        "userId": "1",
        "name": "张同学",
        "avatarUrl": "https://oss.xxx.com/avatar/1.jpg",
        "distance": "1.2km",
        "city": "北京市",
        "canSkills": ["Python", "数据分析", "爬虫"],
        "wantSkills": ["Java Spring Boot", "吉他"],
        "bio": "计算机大三，Python写了三年...",
        "matchScore": 94,
        "aiScore": 0.87,
        "likeCount": 47
      }
    ]
  }
}
```

| 字段 | 说明 |
| ---- | ---- |
| city | 城市名（高德逆地理编码获取，未设置位置时为空字符串） |
| matchScore | 综合匹配度 (0-100)，Java 互补度分 + AI 精排分的加权结果 |
| aiScore | AI 模型预测的匹配置信度 (0-1)，AI 未介入时为 null |

### 3.3 获取匹配用户名片（弹窗）

```
GET /api/matching/users/{userId}/card
```

> **说明**：此接口在 `GET /api/user/card/{userId}`（见 2.0）基础上扩展了匹配相关字段（`distance`、`matchScore`）。用于发现页点击头像弹出名片时，同时展示距离和匹配度。其他场景（社区、通知等）使用 2.0 即可。

> 无需登录可访问，但未登录时 `likeCount` 返回 0。

**响应**

```json
{
  "code": 200,
  "data": {
    "userId": "1",
    "name": "张同学",
    "avatarUrl": "...",
    "signature": "代码改变世界",
    "likeCount": 47,
    "postCount": 5,
    "canSkills": ["Python", "数据分析"],
    "wantSkills": ["Java Spring Boot", "吉他"],
    "distance": "1.2km",
    "city": "北京市",
    "matchScore": 94,
    "hasPendingRequest": false
  }
}
```

> `matchScore` 仅从匹配推荐页进入时有值，其他场景为 null。`distance` 需要调用方传入当前用户坐标计算，未传时为 null。

### 3.4 获取用户完整主页

> 需要登录。查看自己时直接放行；查看他人需要双方已有 `accepted` 交换请求，否则返回 403。

```
GET /api/matching/users/{userId}/profile
```

**响应**

```json
{
  "code": 200,
  "data": {
    "userId": "1",
    "name": "张同学",
    "avatarUrl": "...",
    "coverUrl": "...",
    "signature": "代码改变世界",
    "bio": "计算机大三，Python写了三年...",
    "contactInfo": "微信号：wx_skillmatch_zhang3",
    "canSkills": ["Python", "数据分析", "爬虫", "机器学习入门", "Flask", "SQL"],
    "wantSkills": ["Java Spring Boot", "吉他进阶", "摄影", "日语N3"],
    "hobbies": [
      {"name": "篮球", "icon": "🏀"},
      {"name": "吉他", "icon": "🎸"}
    ],
    "gallery": ["url1", "url2"],
    "likeCount": 128,
    "postCount": 5,
    "activities": [
      {
        "type": "post",
        "content": "发布了帖子《找Python搭子一起刷LeetCode》",
        "createdAt": "2026-05-12T14:30:00"
      }
    ]
  }
}
```

> `activities` 返回最近 20 条；`contactInfo` 仅双方交换已同意时返回。

---

## 四、社区模块 `/api/community`

### 4.1 帖子列表

```
GET /api/community/posts
```

| 参数 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | ---------------------- |
| tag | string | 否 | 按标签筛选 |
| sort | string | 否 | latest=最新（默认） / hot=最热 |
| keyword | string | 否 | 搜索标题、正文关键词 |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 10 |

**响应**

```json
{
  "code": 200,
  "data": {
    "total": 120,
    "page": 1,
    "size": 10,
    "list": [
      {
        "id": "1",
        "author": {
          "userId": "1",
          "name": "张同学",
          "avatarUrl": "..."
        },
        "title": "找Python搭子一起刷LeetCode",
        "body": "最近在准备春招...",
        "tags": ["Python", "找搭子", "刷题"],
        "likeCount": 12,
        "commentCount": 3,
        "liked": false,
        "createdAt": "2026-05-14T14:30:00"
      }
    ]
  }
}
```

### 4.2 发布帖子

> 需要登录。每人每小时 5 篇上限，超限返回 429。

```
POST /api/community/posts
```

**请求体**

```json
{
  "title": "找Python搭子一起刷LeetCode",
  "body": "最近在准备春招...",
  "tags": ["Python", "找搭子", "刷题"]
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | ---------------- |
| title | string | 是 | 标题，1-64字符 |
| body | string | 是 | 正文，1-5000字符 |
| tags | array | 否 | 标签数组，≤5个，每个≤16字符 |

> 标签存入前统一转为小写。

### 4.3 帖子详情

```
GET /api/community/posts/{postId}
```

**响应**

```json
{
  "code": 200,
  "data": {
    "id": "1",
    "author": {
      "userId": "1",
      "name": "张同学",
      "avatarUrl": "..."
    },
    "title": "找Python搭子一起刷LeetCode",
    "body": "最近在准备春招...",
    "tags": ["Python", "找搭子", "刷题"],
    "likeCount": 12,
    "commentCount": 3,
    "liked": false,
    "createdAt": "2026-05-14T14:30:00",
    "updatedAt": null
  }
}
```

### 4.4 编辑帖子

> 需要登录。仅作者本人可编辑。

```
PUT /api/community/posts/{postId}
```

**请求体**

```json
{
  "title": "修改后的标题",
  "body": "修改后的正文...",
  "tags": ["Python", "刷题"]
}
```

> 所有字段均可选，不传不更新。校验规则同发布。

### 4.5 删除帖子

> 需要登录。仅作者本人可删除。

```
DELETE /api/community/posts/{postId}
```

**响应**

```json
{
  "code": 200,
  "message": "帖子已删除",
  "data": null
}
```

### 4.6 评论列表

```
GET /api/community/posts/{postId}/comments?page=1&size=20
```

按评论时间正序排列。

**响应**

```json
{
  "code": 200,
  "data": {
    "total": 3,
    "page": 1,
    "size": 20,
    "list": [
      {
        "id": "1",
        "user": {
          "userId": "2",
          "name": "刘英语",
          "avatarUrl": "..."
        },
        "body": "收藏了！请问每天大概投入多久？",
        "createdAt": "2026-05-14T16:00:00"
      }
    ]
  }
}
```

### 4.7 发表评论

> 需要登录。每个帖子每人每小时最多 10 条评论。

```
POST /api/community/posts/{postId}/comments
```

Content-Type: `text/plain`

请求体为纯文本字符串：

```
收藏了！请问每天大概投入多久？
```

| 说明 |
| ------------ |
| 评论内容，1-500字符 |

### 4.8 删除评论

> 需要登录。仅评论者本人或帖子作者可删除。

```
DELETE /api/community/posts/{postId}/comments/{commentId}
```

---

## 五、通知模块 `/api/notification`

> 当前使用 `ContactRequestController` 实现，路径统一为 `/api/notification`。

### 5.1 收到的交换请求

> 需要登录

```
GET /api/notification/requests/received?status=1
```

| 参数 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | ---------------------------------- |
| status | int | 否 | 筛选状态：1=待处理 / 2=已同意 / 3=已拒绝。不传返回全部 |

**响应**

```json
{
  "code": 200,
  "data": [
    {
      "id": "1",
      "fromUser": {
        "id": "1",
        "name": "张同学",
        "avatarUrl": "..."
      },
      "toUser": null,
      "reason": "看到你也在刷题，想一起互相code review",
      "status": 1,
      "createdAt": "2026-05-14T14:30:00"
    }
  ]
}
```

> `fromUser` 为发送请求的用户信息；`toUser` 为 null（收到方向不需要）。`status`：1=待处理、2=已同意、3=已拒绝。

### 5.2 已发送的交换请求

> 需要登录

```
GET /api/notification/requests/sent?status=1
```

| 参数 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | ---------------------------------- |
| status | int | 否 | 筛选状态：1=待处理 / 2=已同意 / 3=已拒绝。不传返回全部 |

**响应**

```json
{
  "code": 200,
  "data": [
    {
      "id": "2",
      "fromUser": null,
      "toUser": {
        "id": "2",
        "name": "李同学",
        "avatarUrl": "..."
      },
      "reason": "你好，方便加个微信互相code review吗？",
      "status": 1,
      "createdAt": "2026-05-14T15:00:00"
    }
  ]
}
```

> `toUser` 为接收请求的用户信息；`fromUser` 为 null（已发送方向不需要）。

### 5.3 发起交换请求

> 需要登录。不能给自己发请求；同一对用户间只能有 1 个 pending 请求。

```
POST /api/notification/requests
```

**请求体**

```json
{
  "toUserId": "28324247242",
  "reason": "你好，方便加个微信互相code review吗？"
}
```

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | ------------ |
| toUserId | string | 是 | 接收方用户 ID |
| reason | string | 是 | 请求理由，1-150字符 |

### 5.4 同意交换请求

> 需要登录。**仅接收方（toUserId == 当前用户）** 可操作。

```
POST /api/notification/requests/{requestId}/accept
```

**响应**（返回对方的联系方式）

```json
{
  "code": 200,
  "data": "微信号：wx_skillmatch_zhang3"
}
```

> 同意后双方均可查看对方的完整主页。

### 5.5 拒绝交换请求

> 需要登录。**仅接收方** 可操作。

```
POST /api/notification/requests/{requestId}/decline
```

> 拒绝后请求状态更新为 3（已拒绝）。

### 5.6 未读消息数

> 需要登录

```
GET /api/notification/unread-count
```

**响应**

```json
{
  "code": 200,
  "data": 2
}
```

> 返回 pending 状态的交换请求数量，用于导航栏角标展示。

---

## 六、通用模块 `/api/common`

### 6.1 文件上传

> 需要登录。仅限 jpg / png / webp 图片，单文件 ≤ 10MB。
> **状态：待实现**（OSS 上传逻辑已在 `OssUtil` 中封装，需新增 CommonController 暴露接口）。

```
POST /api/common/upload
```

Content-Type: `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
| ---- | ---- | -- | ---- |
| file | File | 是 | 图片文件 |

**成功响应**

```json
{
  "code": 200,
  "data": {
    "url": "https://oss.xxx.com/images/abc123.jpg"
  }
}
```

**校验失败**

```json
{
  "code": 400,
  "message": "仅支持 jpg/png/webp 格式",
  "data": null
}
```

---

## 七、AI 匹配引擎接口（Python 侧）

> **状态：待实现**。技术栈：FastAPI + Uvicorn + Pydantic 数据校验 + LightGBM 模型推理。以下为 Python AI 引擎的接口契约，供 Python 端开发参考。

> **容错设计**：Java 端通过 RestClient 同步调用，超时 500ms。AI 引擎不可用时自动降级为 Java 规则排序。

### 7.1 精排请求

Java 后端在候选用户 ≥ 20 人时，将粗排后的用户列表发送给 AI 引擎进行精排。请求体和响应体由 Pydantic 模型进行类型校验。

```
POST http://localhost:5000/api/rank
```

**请求体**

```json
{
  "userId": "zhangsan",
  "candidates": [
    {
      "userId": "lisi",
      "canMatchCount": 3,
      "wantMatchCount": 2,
      "hobbyOverlapCount": 1,
      "distanceKm": 1.2,
      "likeCount": 47,
      "postCount": 5,
      "lastLoginDays": 1
    }
  ]
}
```

| 特征字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| canMatchCount | int | 对方想学 ∩ 我方会的数量 |
| wantMatchCount | int | 对方会 ∩ 我方想学的数量 |
| hobbyOverlapCount | int | 共同爱好数量 |
| distanceKm | float | 距离（km） |
| likeCount | int | 对方获赞数 |
| postCount | int | 对方发帖数 |
| lastLoginDays | int | 距上次登录天数 |

**响应**

```json
{
  "code": 200,
  "data": [
    {"userId": "lisi", "score": 0.87},
    {"userId": "wangwu", "score": 0.72}
  ]
}
```

> 按 `score` 降序排列。AI 引擎不可用时，Java 侧降级为纯规则排序。

### 7.2 健康检查

```
GET http://localhost:5000/api/health
```

> Java 侧启动时和定期（每 5 分钟）调用此接口，检测 AI 引擎是否在线。

**响应**

```json
{
  "status": "ok",
  "model_version": "1.0.0"
}
```

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| status | string | `ok` 表示引擎正常 |
| model_version | string | 语义化版本号，格式 `主.次.修订`，与模型文件名对应 |

> 模型版本号约定：`model/lambdamart_v{version}.txt`，如 `lambdamart_v1.0.0.txt`。

---

## 附录

### A. 交换请求状态机

```
pending (1) ──accept──▶ accepted (2)（双方可见联系方式）
    │
    └─decline──▶ declined (3)
```

### B. 请求频率限制汇总

| 接口 | 限制 |
| ---- | ----------------------- |
| 主页点赞 | 不能给自己点赞，不能重复点赞同一人 |
| 帖子点赞 | 不能重复点赞同一帖子 |
| 发起交换请求 | 对同一用户只能有 1 个 pending 请求 |
| 发帖 | 每人每小时 5 篇 |
| 评论 | 每人每帖每小时 10 条 |

### C. "最热"排序公式

社区帖子 `sort=hot` 时使用 Hacker News 衰减算法：

```
score = (likeCount × 2 + commentCount × 3) / pow(hours + 2, 1.5)
```

### D. 权限矩阵

| 接口 | 未登录 | 已登录 | 交换已同意 |
| ---- | ---- | ---- | ---- |
| 用户名片弹窗 `GET /api/user/card/{userId}` | ✅ (likeCount=0) | ✅ | ✅ |
| 匹配用户名片 `GET /api/matching/users/{userId}/card` | ✅ (likeCount=0) | ✅ | ✅ |
| 发现页用户列表 | ✅ | ✅ | ✅ |
| 用户完整主页 | ❌ | ❌（自己除外）| ✅ |
| 发起交换请求 | ❌ | ✅ | — |
| 我的主页（自己看自己） | ❌ | ✅ | — |
| 文件上传 | ❌ | ✅ | — |

### E. 实现状态总览

| 模块 | 接口 | 状态 |
| ---- | ---- | ---- |
| 认证 | 注册/登录/刷新/注销 | ✅ 已完成 |
| 用户 | 资料/头像/密码/位置/技能/爱好/相册 | ✅ 已完成 |
| 用户 | 名片弹窗 `GET /api/user/card/{userId}` | ❌ 待实现 |
| 用户 | 动态 | ❌ 待实现 |
| 用户 | 注销账号（数据清理） | ⚠️ 部分实现 |
| 点赞 | 通用点赞（主页+帖子） | ✅ 已完成 |
| 匹配发现 | 推荐列表/名片/完整主页 | ❌ 待实现 |
| 社区 | 帖子CRUD/评论/标签 | ✅ 已完成 |
| 通知 | 交换请求收发/同意/拒绝 | ✅ 已完成 |
| 通用 | 文件上传 | ❌ 待实现 |
| AI引擎 | 精排/健康检查 | ❌ 待实现 |

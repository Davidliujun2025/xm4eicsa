# 智能对话工作台 — 设计 Spec

> **锁定所有接口命名、数据字段、流程逻辑。后续三份文档以此为准。**

---

## 1. 范围

| 板块 | 归属 | 说明 |
|------|------|------|
| 对话 CRUD + 逐步生成 + 编辑保存 | **我** | 完整实现 |
| Token 消耗查询 | Token 同学 | 我定义接口契约，对方实现 |
| 收藏/取消收藏 | 收藏同学 | 我定义接口契约，对方实现 |

---

## 2. 接口清单（唯一命名源）

| # | 方法 | 路径 | 归属 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/conversations` | 我 |
| 2 | `GET` | `/api/v1/conversations/{id}` | 我 |
| 3 | `POST` | `/api/v1/conversations` | 我 |
| 4 | `POST` | `/api/v1/conversations/{id}/generate-next` | 我 |
| 5 | `PUT` | `/api/v1/conversations/{id}` | 我 |
| 6 | `GET` | `/api/v1/token/usage` | Token 同学 |
| 7 | `GET` | `/api/v1/favorites` | 收藏同学 |
| 8 | `POST` | `/api/v1/favorites` | 收藏同学 |
| 9 | `DELETE` | `/api/v1/favorites/{id}` | 收藏同学 |

路径一律 `/api/v1/` 前缀，kebab-case。

---

## 3. 统一响应格式

```json
{ "code": 200, "message": "success", "data": {...} }
```

| 字段 | 类型 |
|------|------|
| `code` | int |
| `message` | string |
| `data` | object / array / null |

---

## 4. 状态码

### 4.1 HTTP 状态码

| 状态码 | 含义 |
|--------|------|
| 200 | 成功（业务错误也返回 200，通过 code 区分） |
| 400 | 参数校验失败 |
| 401 | 未登录 |
| 403 | Token 额度耗尽 |
| 404 | 资源不存在 |
| 409 | 业务冲突 |
| 500 | 服务器内部错误 |

### 4.2 业务错误码

| code | HTTP | message | 场景 |
|------|------|---------|------|
| 200 | 200 | success | 正常 |
| 40001 | 400 | 参数校验失败：{字段} | 请求格式不对 |
| 40002 | 400 | 平台和客户问题不能为空 | 创建对话缺必填项 |
| 40003 | 400 | 当前对话状态不允许继续生成 | status 不是 generating |
| 40004 | 400 | 五步法已完整生成，无法继续 | 已有 5 条 step |
| 40301 | 403 | Token 额度已耗尽，请稍后再试 | 当日额度用尽 |
| 40401 | 404 | 对话不存在 | conversationId 无效 |
| 40402 | 404 | 收藏记录不存在 | 取消收藏时 ID 无效 |
| 40901 | 409 | 该话术已收藏，请勿重复操作 | 重复收藏同一 step |
| 40100 | 401 | 未登录或登录已过期 | Token 无效 |
| 50001 | 200 | AI生成失败，请稍后重试 | Qwen 返回错误 |
| 50002 | 200 | AI返回格式异常 | JSON 解析失败 |
| 50401 | 200 | AI生成超时（超过15s），请稍后重试 | 超时 |

---

## 5. 数据模型（字段唯一命名源）

### 5.1 conversation

| 字段 | 类型 | 约束 |
|------|------|------|
| `id` | BIGINT PK | 自增 |
| `user_id` | BIGINT | NOT NULL |
| `platform` | VARCHAR(32) | NOT NULL，10 个平台之一 |
| `question` | VARCHAR(500) | NOT NULL，≤500 字符 |
| `category` | VARCHAR(32) | NULLABLE，AI 生成后回填 |
| `status` | VARCHAR(16) | NOT NULL，`generating` / `success` / `failed` |
| `created_at` | DATETIME | NOT NULL，北京时间 |
| `updated_at` | DATETIME | NOT NULL，北京时间 |

### 5.2 conversation_step

| 字段 | 类型 | 约束 | 可编辑 |
|------|------|------|--------|
| `id` | BIGINT PK | 自增 | — |
| `conversation_id` | BIGINT | NOT NULL，应用层引用 | — |
| `user_id` | BIGINT | NOT NULL，冗余 | — |
| `step_order` | TINYINT | NOT NULL，CHECK 1-5 | ❌ |
| `step_type` | VARCHAR(16) | NOT NULL | ❌ |
| `title` | VARCHAR(64) | NULLABLE | ❌ |
| `label` | VARCHAR(32) | NULLABLE | ✅ |
| `tags` | JSON | NULLABLE，`["a","b"]` | ✅ |
| `section` | VARCHAR(32) | NULLABLE | ❌ |
| `body` | TEXT | NOT NULL | ✅ |
| `risk_warning` | JSON | NULLABLE，仅 step3 有值 | ❌ |
| `status` | VARCHAR(64) | NULLABLE，展示字段 | ❌ |
| `created_at` | DATETIME | NOT NULL | — |
| `updated_at` | DATETIME | NOT NULL | — |

**可编辑规则**：所有五步（step_order 1-5）的 `body`、`label`、`tags` 均可编辑，无例外。PUT 接口只更新这三个字段，其他字段传入的值忽略。

### 5.3 favorite

| 字段 | 类型 | 约束 |
|------|------|------|
| `id` | BIGINT PK | 自增 |
| `user_id` | BIGINT | NOT NULL |
| `conversation_id` | BIGINT | NULLABLE，冗余 |
| `step_id` | BIGINT | NOT NULL，UK (user_id, step_id) |
| `step_type` | VARCHAR(16) | NOT NULL |
| `content` | TEXT | NOT NULL，快照 |
| `platform` | VARCHAR(32) | NOT NULL |
| `category` | VARCHAR(32) | NULLABLE |
| `question` | VARCHAR(500) | NULLABLE |
| `collected_at` | DATETIME | NOT NULL，北京时间 |

### 5.4 通用约束

- 三表均**不加 FOREIGN KEY**
- 所有 DATETIME 为北京时间（UTC+8）
- 引擎 InnoDB，CHARSET utf8mb4，COLLATE utf8mb4_0900_ai_ci

---

## 6. 枚举值

### 6.1 平台（10 个固定值，后端硬编码）

```
淘宝, 天猫, 京东, 拼多多, 抖音, 小红书, 快手, 视频号, 微信小店, 其他平台
```

### 6.2 问题分类（7 个，AI 生成后回填）

```
直接咨询, 售后咨询, 物流咨询, 优惠活动咨询, 发票咨询, 适配性咨询, 其他
```

### 6.3 对话状态（3 态）

```
generating → success
generating → failed
```

### 6.4 五步法步骤类型（5 个固定值）

```
意图识别, 回复策略, 推荐话术, 钩子引导, 成功收尾
```

### 6.5 收藏筛选分类（8 个，含"全部"）

```
全部, 直接咨询, 售后咨询, 物流咨询, 优惠活动咨询, 发票咨询, 适配性咨询, 其他
```

---

## 7. 核心流程

### 7.1 创建对话 + 第 1 步

```
POST /api/v1/conversations
{ "platform": "...", "question": "..." }

后端：
1. 校验参数
2. AuthContext.getUserId() → Mock=1
3. INSERT conversation (status="generating")
4. QwenClient.call(buildStep1Prompt())
   → 框架阶段返回 "[AI 生成中...]"
5. INSERT conversation_step (step_order=1)
6. 返回 conversation + steps[1]
```

### 7.2 生成下一步

```
POST /api/v1/conversations/{id}/generate-next
请求体：无

后端：
1. 校验 status="generating" 且已有 steps < 5
2. 读取前序 step 的 body（DB 最新值 = 用户可能已编辑）
3. QwenClient.call(buildStepNPrompt(前序body))
4. INSERT conversation_step (step_order=N)
5. 若 N=5 → UPDATE conversation.status="success"
6. 返回新 step
```

### 7.3 静默保存

```
PUT /api/v1/conversations/{id}
{ "steps": [{ "stepId": ..., "stepOrder": ..., "body": ..., "label": ..., "tags": [...] }, ...] }

后端：
1. 校验对话存在
2. 逐条更新 body、label、tags
3. 忽略 stepType/title/section 等字段
4. 返回 200 OK，无提示
```

---

## 8. 步骤 JSON 响应结构（所有接口统一）

```json
{
  "stepId":        long,
  "stepOrder":     int,          // 1-5
  "stepType":      string,       // 五步法类型之一
  "title":         string,       // "第N步 - 意图识别"
  "label":         string,       // 可编辑
  "tags":          string[],     // 可编辑
  "section":       string,       // "关键词"等
  "body":          string,       // 可编辑
  "riskWarning":   object|null,  // 仅 step3 有值
  "status":        string,       // 展示文本
  "editable":      boolean       // 始终 true
}
```

## 9. Token 信息结构（嵌入在 conversation 响应中）

```json
{
  "currentChatUsage": int,      // 本次消耗
  "totalLimit":       int,      // 每日配额
  "usedToday":        int,      // 今日已用
  "usagePercent":     double    // 百分比 0-100
}
```

## 10. AI 对接契约

```
QwenClient 接口:
  String call(String prompt) throws AiGenerationException

PromptBuilder:
  buildStepNPrompt(stepOrder, platform, question, previousStepBodies[]) → String

框架阶段:
  StubQwenClient 返回 "{ \"body\": \"[AI 生成中...]\", \"label\": \"待生成\", \"tags\": [] }"
  AI 接入后替换为 DashScopeQwenClient
```

## 11. 关键约束（所有文档必须遵守）

- [ ] 所有五步均可编辑（body/label/tags），API 文档 editable 始终 true
- [ ] POST /conversations 只生成第 1 步，不预创建 5 条
- [ ] generate-next 只能从 generating 状态触发
- [ ] 第 5 步生成后自动 status=success
- [ ] 编辑只改 body/label/tags，stepType/title/section 传了也忽略
- [ ] 无 FK 约束
- [ ] 不做级联重新生成
- [ ] 不做后端自动重试
- [ ] 超时 15s，返回 50401
- [ ] 所有时间北京时间

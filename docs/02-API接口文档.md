# 智能对话工作台 — API 接口文档

> **版本**：v1.0
> **日期**：2026-07-17
> **关联需求**：XM4-11
> **Base URL**：`http://localhost:8080/api/v1`

---

## 1. 通用约定

### 1.1 统一响应格式

所有接口返回以下 JSON 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 业务状态码，200 表示成功 |
| `message` | string | 提示信息，成功时为 `"success"` |
| `data` | object / array / null | 业务数据，无数据时为 `null` |

### 1.2 请求格式

所有 POST/PUT 请求需携带：

```
Content-Type: application/json; charset=utf-8
Authorization: Bearer <token>
```

GET/DELETE 请求只需携带 `Authorization` 头。

### 1.3 鉴权

所有接口需要登录后访问。前端在请求头中携带鉴权信息（具体方式待鉴权同学确认后补充）：

```
Authorization: Bearer <token>
```

后端从 Token 中解析当前客服人员的 `userId`。

> **迭代一开发期间**：后端使用 Mock `userId = 1`，鉴权对接时替换为真实取值逻辑。

### 1.4 HTTP 状态码

| 状态码 | 含义 |
|--------|------|
| 200 | 成功（含业务错误时也返回 200，通过 `code` 区分） |
| 400 | 请求参数错误（校验失败） |
| 401 | 未登录 / Token 过期 |
| 403 | Token 额度已耗尽，禁止生成 |
| 404 | 资源不存在（对话/收藏记录未找到） |
| 409 | 业务冲突（如重复收藏） |
| 500 | 服务器内部错误 |

### 1.5 业务错误码

| code | HTTP 状态码 | message | 触发场景 |
|------|-----------|---------|---------|
| 200 | 200 | success | 正常 |
| 40001 | 400 | 参数校验失败：{具体字段} | 请求参数格式不正确 |
| 40002 | 400 | 平台和客户问题不能为空 | 创建对话缺少必填项 |
| 40003 | 400 | 当前对话状态不允许继续生成 | 对话 status 不是 `generating`（如已是 `success`/`failed`） |
| 40004 | 400 | 五步法已完整生成，无法继续 | 当前对话已有 5 条 step 记录 |
| 40301 | 403 | Token 额度已耗尽，请稍后再试 | 当日 Token 使用量已达上限 |
| 40401 | 404 | 对话不存在 | conversationId 无效 |
| 40402 | 404 | 收藏记录不存在 | 取消收藏时 ID 无效 |
| 40901 | 409 | 该话术已收藏，请勿重复操作 | 重复收藏同一步骤 |
| 40100 | 401 | 未登录或登录已过期 | Token 无效/过期 |
| 50001 | 200 | AI 生成失败，请稍后重试 | Qwen API 返回错误（任意单步失败） |
| 50002 | 200 | AI 返回格式异常 | Qwen 返回的 JSON 解析失败 |
| 50401 | 200 | AI 生成超时（超过 15s），请稍后重试 | 单步 Qwen 调用耗时超过 15s |

---

## 2. 接口清单

| 序号 | 方法 | 路径 | 说明 | 板块 | 归属 |
|------|------|------|------|------|------|
| 1 | `GET` | `/api/v1/conversations` | 查询对话历史列表 | 页面初始化 | **我** |
| 2 | `GET` | `/api/v1/conversations/{id}` | 查询单条对话详情（含已生成步骤） | 页面初始化 | **我** |
| 3 | `POST` | `/api/v1/conversations` | 创建对话并生成第 1 步（意图识别） | AI 生成框架 | **我** |
| 4 | `POST` | `/api/v1/conversations/{id}/generate-next` | 基于前序步骤的当前内容，生成下一步骤 | AI 生成框架 | **我** |
| 5 | `PUT` | `/api/v1/conversations/{id}` | 静默保存已生成步骤的编辑内容 | 五步法内容处理 | **我** |
| 6 | `GET` | `/api/v1/token/usage` | 查询今日 Token 消耗 | 页面初始化 | Token 同学 |
| 7 | `GET` | `/api/v1/favorites` | 查询个人话术库收藏列表 | 收藏 | 收藏同学 |
| 8 | `POST` | `/api/v1/favorites` | 收藏某条步骤话术 | 收藏 | 收藏同学 |
| 9 | `DELETE` | `/api/v1/favorites/{id}` | 取消收藏 | 收藏 | 收藏同学 |

### 核心交互流程

```
逐步生成（用户确认驱动）：
  客服输入平台+问题 → POST /conversations
  → 后端生成第 1 步（意图识别）→ 返回 step1
  → 客服查看/编辑 step1 → 点击"确认，生成下一步"
  → POST /conversations/{id}/generate-next（注入 step1 编辑后 body）
  → 后端生成第 2 步 → 返回 step2
  → ...重复至第 5 步生成完成 → conversation.status = success

编辑保存：
  客服修改任意步骤内容 → 停止输入 1s / 失焦
  → PUT /conversations/{id}（提交所有已生成步骤的完整文本）
  → 静默保存，无弹窗提示
  → 所有五步均可编辑，互不影响

收藏（其他同学实现，本文档定义接口契约）：
  点击收藏 → POST /favorites → 按钮变为"已收藏"
  再次点击 → DELETE /favorites/{id} → 按钮恢复"收藏"
```

---

## 3. 接口详情 —— 我负责

### 3.1 查询对话历史列表

```
GET /api/v1/conversations?page=1&size=20&keyword=&category=
```

**请求参数**（Query String）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码，默认 1 |
| `size` | int | 否 | 每页条数，默认 20，最大 50 |
| `keyword` | string | 否 | 搜索关键词，模糊匹配客户问题内容 |
| `category` | string | 否 | 分类筛选。可选值：`全部`/`直接咨询`/`售后咨询`/`物流咨询`/`优惠活动咨询`/`发票咨询`/`适配性咨询`/`其他`。不传或传`全部`表示不过滤 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 28,
    "page": 1,
    "size": 20,
    "list": [
      {
        "id": 1,
        "question": "耳机支持七天无理由退货吗？",
        "platform": "天猫",
        "category": "售后咨询",
        "status": "success",
        "createdAt": "2026-07-17 10:24:00",
        "updatedAt": "2026-07-17 10:24:05"
      }
    ]
  }
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 对话 ID |
| `question` | string | 客户问题内容 |
| `platform` | string | 服务平台 |
| `category` | string | 问题分类，未生成时为 null |
| `status` | string | `generating` / `success` / `failed` |
| `createdAt` | string | 创建时间（北京时间） |
| `updatedAt` | string | 最近更新时间（北京时间） |

---

### 3.2 查询单条对话详情

```
GET /api/v1/conversations/{id}
```

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 对话 ID |

**异常状态行为**：

| 对话 status | 返回行为 |
|------------|---------|
| `generating` | 返回 conversation 基本信息 + 已生成完成的步骤（1-4 条），前端可据此展示生成进度 |
| `failed` | 返回 conversation 基本信息 + 已持久化的步骤（0-4 条） |
| `success` | 返回完整 5 步骤数据 |

**成功响应**（status=success 示例）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "platform": "淘宝",
    "question": "这款蓝牙耳机支持七天无理由退货吗？我刚收到且尚未拆封。",
    "category": "退换货咨询",
    "status": "success",
    "createdAt": "2026-07-17 10:24:00",
    "updatedAt": "2026-07-17 10:25:30",
    "steps": [
      {
        "stepId": 101,
        "stepOrder": 1,
        "stepType": "意图识别",
        "title": "第1步 - 意图识别",
        "label": "客户意图",
        "tags": ["退换货咨询", "理性询问"],
        "section": "关键词",
        "body": "七天无理由、商品状态、退换条件、申请流程",
        "riskWarning": null,
        "status": "已识别淘宝客户问题类型与核心诉求",
        "editable": true
      },
      {
        "stepId": 102,
        "stepOrder": 2,
        "stepType": "回复策略",
        "title": "第2步 - 回复策略",
        "label": "回复方向",
        "tags": ["先确认条件", "再说明流程"],
        "section": "组织建议",
        "body": "先说明淘宝退换货的适用前提，再解释时限、商品状态和配件要求...",
        "riskWarning": null,
        "status": "已生成适用于淘宝的回复路径",
        "editable": true
      },
      {
        "stepId": 103,
        "stepOrder": 3,
        "stepType": "推荐话术",
        "title": "第3步 - 推荐话术",
        "label": "适用场景与语气",
        "tags": ["淘宝退换货", "专业友好"],
        "section": "推荐话术",
        "body": "您好，若淘宝订单页面标注支持七天无理由退货...",
        "riskWarning": {
          "hasRisk": false,
          "description": null,
          "alternative": null
        },
        "status": "话术已生成，可直接用于客服回复",
        "editable": true
      },
      {
        "stepId": 104,
        "stepOrder": 4,
        "stepType": "钩子引导",
        "title": "第4步 - 钩子引导",
        "label": "转化方向",
        "tags": ["服务保障", "继续咨询"],
        "section": "引导建议",
        "body": "可补充淘宝订单的售后保障、发货时效或商品适配建议...",
        "riskWarning": null,
        "status": "已生成符合淘宝场景的继续沟通建议",
        "editable": true
      },
      {
        "stepId": 105,
        "stepOrder": 5,
        "stepType": "成功收尾",
        "title": "第5步 - 成功收尾",
        "label": "收尾语气",
        "tags": ["自然礼貌", "持续服务"],
        "section": "结束语建议",
        "body": "您可以放心参考订单页的售后说明；如果后续还有退换货或使用方面的问题，我也会继续协助您处理。",
        "riskWarning": null,
        "status": "淘宝五步话术已完整生成",
        "editable": true
      }
    ],
    "tokenInfo": {
      "currentChatUsage": 120,
      "totalLimit": 50000,
      "usedToday": 12500,
      "usagePercent": 25.0
    }
  }
}
```

**步骤对象通用字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `stepId` | long | 步骤记录主键，收藏时需用到此 ID |
| `stepOrder` | int | 步骤序号 1-5 |
| `stepType` | string | `意图识别` / `回复策略` / `推荐话术` / `钩子引导` / `成功收尾` |
| `title` | string | 展示标题，如 `"第1步 - 意图识别"`（后端始终设置，不会为 null） |
| `label` | string | 内容类别标签，最大 32 字符（**所有步骤均可编辑**） |
| `tags` | string[] | 标签数组，每项最大 32 字符，最多 10 项（**所有步骤均可编辑**） |
| `section` | string | 内容区块标题，如 `"关键词"`（后端始终设置，不会为 null） |
| `body` | string | 核心内容文本，TEXT 类型最大 64KB（**所有步骤均可编辑**） |
| `riskWarning` | object | 平台风险提示，**仅第 3 步（推荐话术）有值**，其余步骤为 `null` |
| `status` | string | 步骤状态的展示文本，最大 64 字符，非机器状态码 |
| `editable` | boolean | 是否可编辑，**始终为 `true`**（计算字段，非数据库列，所有五步均可编辑） |

**riskWarning 结构**（仅 stepOrder=3 时有值）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `hasRisk` | boolean | 是否存在平台表达风险 |
| `description` | string | 风险描述，无风险时为 `null` |
| `alternative` | string | 替代表达建议，无风险时为 `null` |

```json
// 无风险时
{ "hasRisk": false, "description": null, "alternative": null }

// 有风险时（如小红书平台）
{
  "hasRisk": true,
  "description": "话术中存在可能不适合该平台的营销表达",
  "alternative": "建议改为更中性的描述，以商品页与平台规则为准"
}
```

**错误响应**：

```json
{ "code": 40401, "message": "对话不存在", "data": null }
```

---

### 3.3 创建对话并生成第 1 步（意图识别）

```
POST /api/v1/conversations
```

客服输入平台和客户问题后，创建一条新对话并生成五步法的第 1 步。框架阶段返回占位文本，AI 接入后返回真实 AI 生成内容。

**请求体**（JSON）：

```json
{
  "platform": "淘宝",
  "question": "这款蓝牙耳机支持七天无理由退货吗？我刚收到且尚未拆封。"
}
```

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|---------|
| `platform` | string | **是** | 不能为空，必须是支持的平台之一 |
| `question` | string | **是** | 不能为空或纯空格，最大 500 字符 |

**后端处理流程**：

1. 参数校验（platform / question 必填、question ≤ 500 字符）
2. `AuthContext.getUserId()` 获取当前客服 ID（框架阶段 Mock=1）
3. 插入 `conversation` 记录，`status = "generating"`
4. `PromptBuilder.buildStep1Prompt(platform, question)` 构建 Prompt
5. `QwenClient.call(prompt)` 获取 AI 生成结果（框架阶段返回占位文本）
6. 解析结果 → 插入 `conversation_step` 记录（`step_order = 1`）
7. 组装响应返回

**成功响应**（仅返回第 1 步，后续步骤通过 `generate-next` 逐步生成）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "platform": "淘宝",
    "question": "这款蓝牙耳机支持七天无理由退货吗？我刚收到且尚未拆封。",
    "category": null,
    "status": "generating",
    "createdAt": "2026-07-17 10:24:00",
    "updatedAt": "2026-07-17 10:24:00",
    "steps": [
      {
        "stepId": 101,
        "stepOrder": 1,
        "stepType": "意图识别",
        "title": "第1步 - 意图识别",
        "label": "待生成",
        "tags": [],
        "section": "关键词",
        "body": "[AI 生成中...]",
        "riskWarning": null,
        "status": "等待 AI 生成",
        "editable": true
      }
    ],
    "tokenInfo": {
      "currentChatUsage": 0,
      "totalLimit": 50000,
      "usedToday": 0,
      "usagePercent": 0
    }
  }
}
```

> **说明**：响应结构与 3.2 详情接口完全一致（`id` + 基本信息 + `steps[]` + `tokenInfo`），便于前端统一处理。区别在于此响应仅包含 1 个 step，`category` 尚未生成，`tokenInfo` 为框架阶段 Mock 值。

**错误响应**：

```json
// 缺少必填项
{ "code": 40002, "message": "平台和客户问题不能为空", "data": null }

// Token 额度耗尽
{ "code": 40301, "message": "Token 额度已耗尽，请稍后再试", "data": null }
```

---

### 3.4 生成下一步骤

```
POST /api/v1/conversations/{id}/generate-next
```

客服在查看/编辑当前步骤后，点击"确认，生成下一步"触发此接口。后端读取所有已生成步骤的**当前 body**（即用户可能已编辑过的内容），注入 Prompt 后生成下一步骤。框架阶段返回占位文本。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 对话 ID |

**请求体**：无。所有上下文从数据库读取。

**前置条件**：

| 条件 | 不满足时返回 |
|------|-------------|
| conversation 存在 | `40401` 对话不存在 |
| conversation.status = `generating` | `40003` 当前对话状态不允许继续生成 |
| 已生成步骤数 < 5 | `40004` 五步法已完整生成，无法继续 |

**后端处理流程**：

1. 校验对话存在 + status = `generating`
2. 查询已有 `conversation_step` 记录，确认下一步 `step_order ≤ 5`
3. 取所有前序 step 的**当前 body**（数据库最新值，即用户可能已编辑的内容）
4. `PromptBuilder.buildStepNPrompt(stepOrder, platform, question, previousStepBodies)` 构建 Prompt
5. `QwenClient.call(prompt)` 获取 AI 生成结果（框架阶段返回占位文本）
6. 解析结果 → 插入 `conversation_step` 记录（`step_order = N`）
7. 若 `N = 5` → 更新 `conversation.status = "success"`
8. 返回新生成的 step

**成功响应**（以生成第 2 步为例）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "stepId": 102,
    "stepOrder": 2,
    "stepType": "回复策略",
    "title": "第2步 - 回复策略",
    "label": "待生成",
    "tags": [],
    "section": "组织建议",
    "body": "[AI 生成中...]",
    "riskWarning": null,
    "status": "等待 AI 生成",
    "editable": true
  }
}
```

> **注意**：当 `stepOrder = 5` 生成成功后，后端自动将 `conversation.status` 更新为 `success`。

**关键设计**：

- Prompt 注入的是**数据库中最新的 step body**（用户编辑保存后的内容），不是 AI 原始输出
- 客服修正第 1 步的意图识别后，第 2 步的策略会基于修正后的意图生成
- 若客服在触发 `generate-next` 前未通过 PUT 保存编辑，`generate-next` 读取的是上次 PUT 保存的版本（或 AI 原始输出），前端应在用户点击"下一步"前确保通过 PUT 保存最新编辑

**错误响应**：

```json
// 对话状态不允许继续生成
{ "code": 40003, "message": "当前对话状态不允许继续生成", "data": null }

// 五步法已完成
{ "code": 40004, "message": "五步法已完整生成，无法继续", "data": null }

// AI 生成失败
{ "code": 50001, "message": "AI 生成失败，请稍后重试", "data": null }

// 超时
{ "code": 50401, "message": "AI 生成超时（超过 15s），请稍后重试", "data": null }
```

---

### 3.5 保存已生成步骤的编辑内容（静默保存）

```
PUT /api/v1/conversations/{id}
```

客服修改任意步骤内容后，前端通过防抖（停止输入 1 秒后）或失焦触发，**一次性提交所有已生成步骤的完整最新文本**。保存全程静默，不弹提示。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 对话 ID |

**请求体**（JSON）：

```json
{
  "steps": [
    {
      "stepId": 101,
      "stepOrder": 1,
      "body": "七天无理由、商品状态、退换条件、申请流程",
      "label": "客户意图",
      "tags": ["退换货咨询", "理性询问"]
    },
    {
      "stepId": 102,
      "stepOrder": 2,
      "body": "先确认客户的耳机是否已拆封，再说明七天无理由退货的核心条件。",
      "label": "回复方向",
      "tags": ["先确认条件", "再说明流程"]
    }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `steps` | array | **是** | 所有已生成步骤的完整数组（1-5 条，取决于当前生成进度），按 `stepOrder` 排列 |
| `steps[].stepId` | long | **是** | 步骤记录 ID |
| `steps[].stepOrder` | int | **是** | 步骤序号，用于校验顺序（后端不依赖此值做更新条件，仅做校验） |
| `steps[].body` | string | **是** | 编辑后的步骤核心内容，最大 64KB（TEXT） |
| `steps[].label` | string | 否 | 编辑后的内容标签，最大 32 字符 |
| `steps[].tags` | string[] | 否 | 编辑后的标签数组，每项最大 32 字符，最多 10 项 |

**后端处理规则**：

- 只更新 `body`、`label`、`tags` 三个可编辑字段
- `stepType`、`stepOrder`、`title`、`section` 等结构性字段**不接受修改**，请求体中传入了也会被忽略
- 所有五步（step_order 1-5）**均可编辑，无例外**
- 每次 PUT 只需提交当前已生成的步骤，未生成的步骤（尚无 stepId）不传

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误响应**：

```json
{ "code": 40401, "message": "对话不存在", "data": null }
```

---

## 4. 接口契约 —— 其他同学负责

以下接口由其他同学实现，接口路径、请求/响应格式、错误码由本文档定义以确保前后端一致。

### 4.1 查询今日 Token 消耗

```
GET /api/v1/token/usage
```

**归属**：Token 同学（XM4-14/15）

**响应格式**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "currentChatUsage": 150,
    "totalLimit": 50000,
    "usedToday": 12500,
    "usagePercent": 25.0
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `currentChatUsage` | int | 本次对话消耗的 Token 数 |
| `totalLimit` | int | 每日最大配额 |
| `usedToday` | int | 今日累计已消耗 |
| `usagePercent` | double | 已用百分比（0-100），供前端渲染进度条 |

> **框架阶段**：此接口返回 Mock 固定值 `{ currentChatUsage: 0, totalLimit: 50000, usedToday: 0, usagePercent: 0 }`，Token 同学就绪后替换为真实数据源。

---

### 4.2 查询个人话术库收藏列表

```
GET /api/v1/favorites?page=1&size=20&keyword=&category=
```

**归属**：收藏同学（XM4-12）

**请求参数**（Query String）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码，默认 1 |
| `size` | int | 否 | 每页条数，默认 20，最大 50 |
| `keyword` | string | 否 | 搜索关键词，模糊匹配话术内容和客户问题 |
| `category` | string | 否 | 分类筛选。可选值：`全部`/`直接咨询`/`售后咨询`/`物流咨询`/`优惠活动咨询`/`发票咨询`/`适配性咨询`/`其他` |

> **排序**：默认按 `collected_at` 倒序排列（最近收藏的在前）。

**响应格式**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "size": 20,
    "list": [
      {
        "id": 1,
        "id": 1,
        "stepId": 103,
        "stepType": "推荐话术",
        "content": "您好，若淘宝订单页面标注支持七天无理由退货...",
        "platform": "淘宝",
        "category": "退换货咨询",
        "question": "这款蓝牙耳机支持七天无理由退货吗？",
        "collectedAt": "2026-07-17 10:25:00"
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 收藏记录 ID |
| `conversationId` | long | 所属对话 ID |
| `stepId` | long | 关联的步骤 ID（对应 `conversation_step.id`） |
| `stepType` | string | 步骤类型：`意图识别` / `回复策略` / `推荐话术` / `钩子引导` / `成功收尾` |
| `content` | string | 话术内容快照 |
| `platform` | string | 服务平台 |
| `category` | string | 问题分类 |
| `question` | string | 关联的客户问题 |
| `collectedAt` | string | 收藏时间（北京时间） |

---

### 4.3 收藏话术

```
POST /api/v1/favorites
```

**归属**：收藏同学（XM4-12）

**请求体**（JSON）：

```json
{
  "id": 1,
  "stepId": 103,
  "stepType": "推荐话术",
  "content": "您好，若淘宝订单页面标注支持七天无理由退货...",
  "platform": "淘宝",
  "category": "退换货咨询",
  "question": "这款蓝牙耳机支持七天无理由退货吗？"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `conversationId` | long | 否 | 对话 ID（冗余字段，可为空） |
| `stepId` | long | **是** | 步骤记录 ID（对应 `conversation_step.id`）。唯一约束 `(user_id, step_id)` 防重复收藏 |
| `stepType` | string | 是 | 步骤类型 |
| `content` | string | 是 | 话术内容快照（收藏时前端传递页面当前最新文本） |
| `platform` | string | 是 | 服务平台 |
| `category` | string | 否 | 问题分类（快照），如 `"退换货咨询"`。可为空 |
| `question` | string | 否 | 关联的客户问题，最大 500 字符 |

**成功响应**：

```json
{ "code": 200, "message": "已收藏到话术库", "data": { "id": 1 } }
```

**重复收藏**：

```json
{ "code": 40901, "message": "该话术已收藏，请勿重复操作", "data": null }
```

---

### 4.4 取消收藏

```
DELETE /api/v1/favorites/{id}
```

**归属**：收藏同学（XM4-12）

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 收藏记录 ID |

**成功响应**：

```json
{ "code": 200, "message": "已取消收藏", "data": null }
```

**收藏不存在**：

```json
{ "code": 40402, "message": "收藏记录不存在", "data": null }
```

---

## 5. 前端对接时序

### 5.1 页面加载

```
前端                                   后端
 │                                      │
 │ GET /api/v1/conversations            │
 │─────────────────────────────────────>│ → 查询当前客服对话历史 → 返回列表
 │<─────────────────────────────────────│
 │                                      │
 │ GET /api/v1/token/usage              │
 │─────────────────────────────────────>│ → 查询 Token 消耗 → 返回用量
 │<─────────────────────────────────────│
 │                                      │
 │ 平台列表前端硬编码，无需后端接口       │
```

### 5.2 逐步生成流程

```
前端                                   后端
 │                                      │
 │ POST /api/v1/conversations           │
 │ { platform, question }               │
 │─────────────────────────────────────>│ → 创建对话 + 生成 step1 → 返回
 │<─────────────────────────────────────│
 │                                      │
 │ [用户查看/编辑 step1]                 │
 │                                      │
 │ PUT /api/v1/conversations/{id}       │（用户停止输入 1s 后自动触发）
 │ { steps: [step1编辑后内容] }          │
 │─────────────────────────────────────>│ → 静默保存 step1
 │<─────────────────────────────────────│
 │                                      │
 │ [用户点击"确认，生成下一步"]           │
 │                                      │
 │ POST /api/v1/conversations/{id}      │
 │   /generate-next                     │
 │─────────────────────────────────────>│ → 读取 step1 当前 body
 │<─────────────────────────────────────│   → 生成 step2 → 返回
 │                                      │
 │ ...重复至 step5 ...                   │
 │                                      │
 │ POST /api/v1/conversations/{id}      │
 │   /generate-next                     │
 │─────────────────────────────────────>│ → 生成 step5
 │<─────────────────────────────────────│   → status = success
```

### 5.3 切换历史对话

```
前端                                   后端
 │                                      │
 │ [用户点击左侧对话列表中的某条记录]     │
 │                                      │
 │ GET /api/v1/conversations/{id}       │
 │─────────────────────────────────────>│ → 查询对话 + 所有已生成步骤
 │<─────────────────────────────────────│ → 返回完整数据，前端回显
```

### 5.4 前端状态管理参考

| 状态 | 按钮/区域显示 | 操作 |
|------|-------------|------|
| 空闲（未生成） | 「生成 AI 回复」蓝色可点击 | 选择平台、输入问题 |
| 平台或问题为空 | 「生成 AI 回复」灰色不可点击 | 提示"请先选择服务平台" |
| 生成中 | 「AI正在生成中...」不可点击 | 不可操作 |
| 超过 5s | 安抚提示「AI正在努力生成回复，请稍候」 | 等待或取消 |
| 超过 15s | 失败提示「AI生成失败，请稍后重试」+「重新生成」按钮 | 可重试，输入保留 |
| 逐步展示中 | 五步法分步展示，可切换步骤 | 可编辑任意步骤、可收藏 |
| 全部完成 | 完整五步法，status=success | 可编辑任意步骤、可收藏 |

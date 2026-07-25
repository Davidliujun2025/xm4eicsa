# token-quota-management

后台 Token 额度管理功能目录。该模块面向后台管理员，用于查看客服账号每日 Token 配额、今日消耗、剩余额度、使用率状态，并支持单个或批量调整客服每日额度。

## 目录结构

```text
features/token-quota-management/
  server/   Spring Boot 后端
  ui/       Vue 管理端页面
  README.md
db/migrations/V4__token_quota_management.sql
```

上传仓库时不需要上传 `target/`、`node_modules/`、`dist/`、`build/`、`*.jar`、`*.class` 等依赖或构建产物。

## 后端启动

后端默认端口：`8081`。

默认启动内存演示模式：

```bash
cd features/token-quota-management/server
mvn spring-boot:run
```

连接 MySQL：

```bash
cd features/token-quota-management/server
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

或使用 jar：

```bash
java -jar target/token-quota-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=mysql
```

数据库连接通过环境变量配置：

```text
TOKEN_QUOTA_DB_URL
TOKEN_QUOTA_DB_USERNAME
TOKEN_QUOTA_DB_PASSWORD
```

代码也兼容仓库 `.env.example` 中的共享变量：

```text
MYSQL_URL
MYSQL_USERNAME
MYSQL_PASSWORD
```

推荐接口前缀：`/api/token-quota`。

兼容旧接口前缀：`/api/admin/token-quotas`。

## 前端启动

```bash
cd features/token-quota-management/ui
npm install
npm run dev
```

前端默认请求 `/api/token-quota`，Vite 会把 `/api` 代理到 `http://localhost:8081`。

如需兼容旧后端路径，可以设置：

```text
VITE_TOKEN_QUOTA_API_PREFIX=/api/admin/token-quotas
```

访问地址：

```text
http://localhost:5173
```

## 接口说明

统一响应格式：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

### 1. 查询客服 Token 额度列表

```text
GET /api/token-quota/accounts?page=1&pageSize=20
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `accountNo` | 否 | 客服账号，支持模糊筛选 |
| `statusCode` | 否 | `NORMAL`、`HIGH_USAGE`、`EXCEEDED_RECOMMENDED` |
| `page` | 否 | 页码，默认 1 |
| `pageSize` | 否 | 每页条数，默认 20，最大 100 |

返回核心字段包括：`accountNo`、`accountName`、`dailyQuota`、`usedTokens`、`remainingTokens`、`overageTokens`、`usageRatePercent`、`aiCallCount`、`businessCount`、`statusCode`、`statusLabel`、`enabled`。

### 2. 查询单个客服额度详情

```text
GET /api/token-quota/accounts/{accountNo}
```

用于管理员点击某个客服账号后查看完整额度和今日消耗情况。

### 3. 查询额度统计汇总

```text
GET /api/token-quota/summary
```

返回账号总数、总配额、今日总消耗、总体使用率、高使用率账号数、超额账号数，供管理端看板卡片展示。

### 4. 修改单个客服每日额度

```text
PUT /api/token-quota/accounts/{accountNo}/daily-quota
```

请求体：

```json
{
  "dailyQuota": 150000,
  "reason": "根据近期AI调用量上调额度",
  "operatorId": "admin-001",
  "operatorName": "后台管理员"
}
```

校验规则：

- `dailyQuota` 不能为空，必须为正整数。
- `dailyQuota` 不能超过系统最大额度，默认 `10000000`。
- `reason`、`operatorId`、`operatorName` 不能为空。

成功后会更新 `customer_token_quota`，并写入 `token_quota_adjustment_log`。

### 5. 批量修改客服每日额度

```text
PUT /api/token-quota/accounts/daily-quota/batch
```

请求体：

```json
{
  "accountNos": ["13800138000", "13900139000"],
  "dailyQuota": 150000,
  "reason": "统一调整演示额度",
  "operatorId": "admin-001",
  "operatorName": "后台管理员"
}
```

成功后每个客服都会生成一条调整日志。

### 6. 记录 Token 消耗

```text
POST /api/token-quota/usage-records
```

该接口主要用于本模块本地演示或内部调用。正式集成时，真实大模型调用消耗建议由个人 Token 消耗模块写入 `token_usage_event`。

请求体：

```json
{
  "accountNo": "13800138000",
  "consumedTokens": 2500,
  "aiCallCount": 1,
  "businessCount": 1
}
```

即使超过每日额度，后端也继续记录消耗，并将账号状态计算为 `EXCEEDED_RECOMMENDED`。

### 7. 查询额度调整日志

```text
GET /api/token-quota/adjustment-logs?page=1&pageSize=20
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `accountNo` | 否 | 客服账号 |
| `startTime` | 否 | 调整开始时间，如 `2026-07-25T00:00:00` |
| `endTime` | 否 | 调整结束时间，如 `2026-07-25T23:59:59` |
| `page` | 否 | 页码 |
| `pageSize` | 否 | 每页条数 |

返回字段包含客服账号、客服名称、调整前额度、调整后额度、操作人、调整原因和调整时间。

## 流程说明

### 页面进入流程

1. 管理员进入 Token 额度管理页面。
2. 前端调用 `GET /api/token-quota/summary` 获取看板汇总数据。
3. 前端调用 `GET /api/token-quota/accounts` 获取客服账号额度列表。
4. 后端从 `customer_service_user` 读取客服账号，从 `customer_token_quota` 读取每日额度，从 `token_usage_event` 汇总今日消耗。
5. 后端计算剩余额度、超额 Token、使用率和状态标签后返回前端。

### 修改额度流程

1. 管理员点击某一客服账号的“修改额度”。
2. 前端展示当前额度，并要求输入新额度和调整原因。
3. 前端提交 `PUT /api/token-quota/accounts/{accountNo}/daily-quota`。
4. 后端校验额度必须为正整数，且不能超过系统最大额度。
5. 后端读取当前额度，更新 `customer_token_quota.daily_token_limit`。
6. 后端写入 `token_quota_adjustment_log`，记录调整前额度、调整后额度、操作人、调整原因和调整时间。
7. 前端刷新列表和汇总卡片，展示最新额度。

### 批量修改流程

1. 管理员勾选多个客服账号。
2. 前端提交 `PUT /api/token-quota/accounts/daily-quota/batch`。
3. 后端先校验所有账号是否存在，再逐个更新额度。
4. 每个成功更新的账号都会生成独立调整日志。
5. 前端展示批量修改结果。

### 使用率状态计算

```text
usageRatePercent = usedTokens / dailyQuota * 100
remainingTokens = max(0, dailyQuota - usedTokens)
overageTokens = max(0, usedTokens - dailyQuota)
```

| 状态码 | 触发条件 | 页面文案 |
| --- | --- | --- |
| `NORMAL` | 使用率低于 80% | 正常 |
| `HIGH_USAGE` | 使用率达到 80%，但未达到 100% | Token 使用率较高 |
| `EXCEEDED_RECOMMENDED` | 使用量达到或超过每日额度 | 已超出建议额度 |

达到或超过额度后，系统不会停止统计 Token 消耗。管理员可以结合消耗量、AI 调用次数和业务量判断是否需要调整额度。

### MySQL 模式数据流

```text
customer_service_user
        |
        | 账号、名称、启用状态
        v
TokenQuotaService  <--- customer_token_quota
        |                    |
        |                    | 每日额度
        |
        +---- token_usage_event
        |          |
        |          | 今日Token消耗、AI调用次数、业务数
        v
前端额度列表、汇总卡片、状态标签
```

修改额度时的数据流：

```text
前端提交新额度
        |
        v
TokenQuotaService
        |
        +---- 更新 customer_token_quota
        |
        +---- 写入 token_quota_adjustment_log
        |
        v
返回修改结果，前端刷新页面
```

## MySQL 表结构说明

最终集成时按仓库 `db/README.md` 使用共享数据库 `xm4`。本模块早期本地说明中出现的 `ai_customer_service` 可视为独立开发库名，上传仓库后建议统一改为 `xm4`。

共享迁移脚本位置：

```text
db/migrations/V4__token_quota_management.sql
```

后端本地初始化脚本位置：

```text
features/token-quota-management/server/src/main/resources/db/mysql/token_quota_schema.sql
```

### 复用表：customer_service_user

来源：登录模块。用途是作为管理员 Token 额度页面的客服账号来源。

本模块读取字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 客服账号内部主键 |
| `account` | 客服登录账号，本模块展示为 `accountNo` |
| `display_name` | 客服名称，本模块展示为 `accountName` |
| `status` | 客服账号状态，`DISABLED` 时页面展示为不可用 |

### 复用表：token_usage_event

来源：个人 Token 消耗查看模块。用途是保存每次大模型调用的 Token 消耗事件。管理员额度列表中的今日已使用 Token、AI 调用次数、业务数、使用率和超额状态都从该表按当天统计。

本模块读取字段：

| 字段 | 说明 |
| --- | --- |
| `user_id` | 客服用户 ID，建议与登录态用户 ID 或客服账号保持一致 |
| `conversation_id` | 会话 ID，用于统计业务数 |
| `total_tokens` | 本次调用总 Token |
| `occurred_at` | 调用发生时间 |
| `status` | 调用状态 |

### 表：customer_token_quota

用途：保存每个客服当前生效的每日 Token 额度。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 自增主键 |
| `user_id` | `VARCHAR(128)` | 客服用户 ID，每人一条当前额度 |
| `daily_token_limit` | `BIGINT` | 每日 Token 额度，`0` 表示未配置 |
| `status` | `VARCHAR(16)` | `ACTIVE` 或 `DISABLED` |
| `updated_by` | `VARCHAR(128)` | 最后修改人 ID |
| `created_at` | `DATETIME(6)` | 创建时间 |
| `updated_at` | `DATETIME(6)` | 更新时间 |

约束：

- `user_id` 唯一。
- `daily_token_limit >= 0`。
- `status` 只能是 `ACTIVE` 或 `DISABLED`。

### 表：token_quota_adjustment_log

用途：保存管理员每次额度调整记录，满足后续资源分析和操作追溯。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 自增主键 |
| `quota_id` | `BIGINT` | 关联 `customer_token_quota.id`，可为空 |
| `user_id` | `VARCHAR(128)` | 客服用户 ID 快照 |
| `account_no` | `VARCHAR(64)` | 客服账号快照 |
| `account_name` | `VARCHAR(64)` | 客服名称快照 |
| `before_daily_token_limit` | `BIGINT` | 调整前每日 Token 额度 |
| `after_daily_token_limit` | `BIGINT` | 调整后每日 Token 额度 |
| `operator_id` | `VARCHAR(128)` | 操作人 ID |
| `operator_name` | `VARCHAR(64)` | 操作人姓名 |
| `reason` | `VARCHAR(255)` | 调整原因 |
| `adjusted_at` | `DATETIME(6)` | 调整时间 |
| `created_at` | `DATETIME(6)` | 日志创建时间 |

索引：

- `(account_no, adjusted_at)`：按客服账号和调整时间查询。
- `(user_id, adjusted_at)`：按用户 ID 和调整时间查询。
- `(adjusted_at)`：按时间倒序分页。

## 与个人 Token 消耗模块的关系

个人 Token 消耗模块负责写入 `token_usage_event` 原始消耗事件；管理员额度管理模块负责读取这些消耗，并维护 `customer_token_quota` 和 `token_quota_adjustment_log`。

正式集成时，建议统一约定登录态中的客服用户 ID，确保以下字段含义一致：

```text
customer_token_quota.user_id
token_usage_event.user_id
前端/网关注入的当前客服 userId
```

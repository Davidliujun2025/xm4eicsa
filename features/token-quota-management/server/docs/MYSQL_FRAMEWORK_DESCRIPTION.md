# 迭代一（7月1日到7月14日截止）框架、SQL代码（若有）

## 个人Token消耗查看

功能目录：`backend`

是否需要数据库：是

数据库：MySQL 8.0.19+（使用行别名 Upsert；`CHECK` 约束要求 8.0.16+）

数据库名：`ai_customer_service`

API 前缀：

- 个人消耗查询：`/api/v1/token-usage/me`
- 内部调用上报：`/internal/v1/token-usage/events`

预计需要几张表：2 张

缓存：不强依赖 Redis。统计、趋势、记录查询直接读取 MySQL；SSE 订阅连接保存在当前进程内存中。

### 表1：token_usage_event

用途：保存每次大模型调用的 Token 消耗事件，为今日统计、周期汇总、趋势、分页记录、调用详情、异常状态提供原始数据。

字段：

- `idempotency_key`：幂等键，主键，防止同一次调用重复计费。
- `user_id`：客服用户 ID，所有个人查询必须携带此条件，保证数据隔离。
- `conversation_id`：会话 ID，可为空。
- `request_id`：模型调用请求 ID，用于详情查询。
- `provider`：厂商，如 `DEEPSEEK`、`KIMI`、`CLAUDE`、`GPT`。
- `model`：模型名称。
- `input_content_length`：用户输入字符数。
- `output_content_length`：AI 输出字符数。
- `input_tokens`：输入 Token。
- `output_tokens`：输出 Token。
- `cached_input_tokens`：缓存输入 Token。
- `total_tokens`：总 Token。
- `response_time_ms`：响应耗时，单位毫秒。
- `status`：调用状态，只允许 `SUCCEEDED`、`FAILED`。
- `occurred_at`：调用发生时间，统一按 UTC 写入。
- `provider_reported_cost`：厂商返回的成本。
- `cost_currency`：成本币种。
- `created_at`：记录创建时间。

约束与索引：

- `idempotency_key` 为主键。
- Token、字符数、响应时间不能为负数。
- `total_tokens >= input_tokens + output_tokens`。
- `(user_id, request_id)`：详情查询索引。
- `(user_id, occurred_at)`：个人时间范围查询索引。
- `(user_id, provider, occurred_at)`：厂商筛选索引。
- `(user_id, model, occurred_at)`：模型筛选索引。

主要操作：

- 内部上报使用 `INSERT ... ON DUPLICATE KEY UPDATE`，由主键保证跨实例幂等。
- 今日、周、月、历史和趋势按 `user_id + occurred_at` 查询。
- 使用记录按时间倒序分页，使用 `LIMIT + OFFSET`。
- 调用详情按 `user_id + request_id` 查询，避免读取其他客服数据。

### 表2：customer_token_quota

用途：保存每个客服当前生效的每日 Token 额度，供使用率、剩余额度、80% 高使用率和额度耗尽状态判断。

字段：

- `id`：自增主键。
- `user_id`：客服用户 ID，每人一条当前额度。
- `daily_token_limit`：每日 Token 额度；`0` 表示未配置可用额度。
- `status`：`ACTIVE` 或 `DISABLED`。
- `updated_by`：最后修改人 ID。
- `created_at`：创建时间。
- `updated_at`：更新时间。

约束与索引：

- `user_id` 唯一，防止同一客服出现多条当前额度。
- `daily_token_limit >= 0`。
- 状态只允许 `ACTIVE`、`DISABLED`。

主要操作：

- `JdbcTokenQuotaResolver` 按 `user_id` 读取 `ACTIVE` 额度。
- 未查询到有效额度时，回退 `TOKEN_MONITOR_DAILY_TOKEN_LIMIT`。
- 额度管理模块可使用 MySQL Upsert 新增或调整额度。

## MySQL 建库建表代码

```sql
CREATE DATABASE IF NOT EXISTS ai_customer_service
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ai_customer_service;

CREATE TABLE IF NOT EXISTS token_usage_event (
    idempotency_key        VARCHAR(160) NOT NULL COMMENT '幂等键',
    user_id                VARCHAR(128) NOT NULL COMMENT '客服用户ID',
    conversation_id        VARCHAR(160) NULL COMMENT '会话ID',
    request_id             VARCHAR(160) NOT NULL COMMENT '模型调用请求ID',
    provider               VARCHAR(32) NOT NULL COMMENT '模型厂商',
    model                  VARCHAR(160) NOT NULL COMMENT '模型名称',
    input_content_length   BIGINT NOT NULL DEFAULT 0 COMMENT '用户输入字符数',
    output_content_length  BIGINT NOT NULL DEFAULT 0 COMMENT 'AI输出字符数',
    input_tokens           BIGINT NOT NULL COMMENT '输入Token',
    output_tokens          BIGINT NOT NULL COMMENT '输出Token',
    cached_input_tokens    BIGINT NOT NULL DEFAULT 0 COMMENT '缓存输入Token',
    total_tokens           BIGINT NOT NULL COMMENT '总Token',
    response_time_ms       BIGINT NOT NULL COMMENT '响应耗时毫秒',
    status                 VARCHAR(16) NOT NULL COMMENT 'SUCCEEDED或FAILED',
    occurred_at            DATETIME(6) NOT NULL COMMENT '调用发生时间，按UTC写入',
    provider_reported_cost DECIMAL(24, 10) NOT NULL DEFAULT 0 COMMENT '厂商报告成本',
    cost_currency          VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT '成本币种',
    created_at             DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (idempotency_key),
    KEY idx_token_usage_user_request (user_id, request_id),
    KEY idx_token_usage_user_time (user_id, occurred_at DESC),
    KEY idx_token_usage_user_provider_time (user_id, provider, occurred_at DESC),
    KEY idx_token_usage_user_model_time (user_id, model, occurred_at DESC),
    CONSTRAINT chk_token_usage_content_length
        CHECK (input_content_length >= 0 AND output_content_length >= 0),
    CONSTRAINT chk_token_usage_tokens
        CHECK (input_tokens >= 0 AND output_tokens >= 0 AND cached_input_tokens >= 0
            AND total_tokens >= input_tokens + output_tokens),
    CONSTRAINT chk_token_usage_response_time CHECK (response_time_ms >= 0),
    CONSTRAINT chk_token_usage_status CHECK (status IN ('SUCCEEDED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='个人模型调用Token使用事件';

CREATE TABLE IF NOT EXISTS customer_token_quota (
    id                BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id           VARCHAR(128) NOT NULL COMMENT '客服用户ID',
    daily_token_limit BIGINT NOT NULL COMMENT '每日Token额度',
    status            VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE或DISABLED',
    updated_by        VARCHAR(128) NULL COMMENT '最后修改人ID',
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                      ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_token_quota_user (user_id),
    CONSTRAINT chk_customer_token_quota_limit CHECK (daily_token_limit >= 0),
    CONSTRAINT chk_customer_token_quota_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='客服个人每日Token额度';
```

## MySQL 主要操作代码

```sql
-- 额度管理模块：新增或调整个人每日额度
INSERT INTO customer_token_quota
    (user_id, daily_token_limit, status, updated_by)
VALUES
    (?, ?, 'ACTIVE', ?) AS new
ON DUPLICATE KEY UPDATE
    daily_token_limit = new.daily_token_limit,
    status = new.status,
    updated_by = new.updated_by;

-- 个人Token模块：读取有效额度
SELECT daily_token_limit
FROM customer_token_quota
WHERE user_id = ? AND status = 'ACTIVE'
LIMIT 1;

-- 今日/周期汇总、趋势的原始数据查询
SELECT *
FROM token_usage_event
WHERE user_id = ?
  AND occurred_at >= ?
  AND occurred_at < ?
ORDER BY occurred_at DESC, idempotency_key;

-- 使用记录分页
SELECT *
FROM token_usage_event
WHERE user_id = ?
  AND occurred_at >= ?
  AND occurred_at < ?
ORDER BY occurred_at DESC, idempotency_key
LIMIT ? OFFSET ?;

-- 调用详情，user_id 条件用于用户隔离
SELECT *
FROM token_usage_event
WHERE user_id = ? AND request_id = ?
ORDER BY occurred_at DESC
LIMIT 1;
```

说明：当前业务统计逻辑在 Java `TokenUsageService` 中完成，数据库保存原始调用事实，不额外建立日汇总表，避免原始数据和汇总数据不一致。

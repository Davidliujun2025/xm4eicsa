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

CREATE TABLE IF NOT EXISTS customer_service_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL DEFAULT '',
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_customer_service_user_account UNIQUE (account),
    CONSTRAINT chk_customer_service_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE IF NOT EXISTS token_usage_event (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    conversation_id VARCHAR(128),
    request_id VARCHAR(128) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(64) NOT NULL,
    input_content_length INT NOT NULL DEFAULT 0,
    output_content_length INT NOT NULL DEFAULT 0,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    cached_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    response_time_ms BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    provider_reported_cost DECIMAL(18, 6) NOT NULL DEFAULT 0,
    cost_currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_token_usage_event_input_len CHECK (input_content_length >= 0),
    CONSTRAINT chk_token_usage_event_output_len CHECK (output_content_length >= 0),
    CONSTRAINT chk_token_usage_event_input_tokens CHECK (input_tokens >= 0),
    CONSTRAINT chk_token_usage_event_output_tokens CHECK (output_tokens >= 0),
    CONSTRAINT chk_token_usage_event_cached_tokens CHECK (cached_input_tokens >= 0),
    CONSTRAINT chk_token_usage_event_total_tokens CHECK (total_tokens >= input_tokens + output_tokens),
    CONSTRAINT chk_token_usage_event_response_time CHECK (response_time_ms >= 0),
    CONSTRAINT chk_token_usage_event_status CHECK (status IN ('SUCCEEDED', 'FAILED')),
    INDEX idx_token_usage_event_user_request (user_id, request_id),
    INDEX idx_token_usage_event_user_time (user_id, occurred_at),
    INDEX idx_token_usage_event_user_provider_time (user_id, provider, occurred_at),
    INDEX idx_token_usage_event_user_model_time (user_id, model, occurred_at)
);

CREATE TABLE IF NOT EXISTS customer_token_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(128) NOT NULL,
    daily_token_limit BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    updated_by VARCHAR(64),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_customer_token_quota_user UNIQUE (user_id),
    CONSTRAINT chk_customer_token_quota_limit CHECK (daily_token_limit >= 0),
    CONSTRAINT chk_customer_token_quota_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE IF NOT EXISTS token_quota_adjustment_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quota_id BIGINT,
    user_id VARCHAR(128) NOT NULL,
    account_no VARCHAR(64) NOT NULL,
    account_name VARCHAR(64) NOT NULL,
    before_daily_token_limit BIGINT NOT NULL,
    after_daily_token_limit BIGINT NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operator_name VARCHAR(64) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    adjusted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_token_quota_log_before_limit CHECK (before_daily_token_limit >= 0),
    CONSTRAINT chk_token_quota_log_after_limit CHECK (after_daily_token_limit >= 0),
    CONSTRAINT fk_token_quota_log_quota
        FOREIGN KEY (quota_id) REFERENCES customer_token_quota(id),
    INDEX idx_token_quota_log_account_time (account_no, adjusted_at),
    INDEX idx_token_quota_log_user_time (user_id, adjusted_at),
    INDEX idx_token_quota_log_adjusted_at (adjusted_at)
);

INSERT INTO customer_token_quota (
    user_id,
    daily_token_limit,
    status,
    updated_by,
    created_at,
    updated_at
)
SELECT
    u.account,
    100000,
    'ACTIVE',
    'system-init',
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
FROM customer_service_user u
WHERE u.status = 'ACTIVE'
ON DUPLICATE KEY UPDATE
    updated_at = customer_token_quota.updated_at;

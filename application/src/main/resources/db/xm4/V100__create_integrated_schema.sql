CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(128) NULL,
    password_hash VARCHAR(100) NOT NULL,
    password VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    role_type VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER_SERVICE',
    last_login_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_phone (phone),
    UNIQUE KEY uk_sys_user_email (email),
    CONSTRAINT chk_sys_user_status CHECK (status IN ('ENABLED', 'DISABLED')),
    CONSTRAINT chk_sys_user_role CHECK (role_type IN ('ADMIN', 'CUSTOMER_SERVICE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS login_page_config (
    id BIGINT NOT NULL,
    brand_name VARCHAR(100) NOT NULL,
    logo_url VARCHAR(500) NOT NULL,
    promo_copy VARCHAR(500) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO login_page_config (id, brand_name, logo_url, promo_copy)
VALUES (1, 'CarePilot AI', '/assets/brand-logo.svg', 'AI 智能客服工作台')
ON DUPLICATE KEY UPDATE brand_name = VALUES(brand_name);

CREATE TABLE IF NOT EXISTS password_reset_session (
    reset_token VARCHAR(64) NOT NULL,
    account_id BIGINT NOT NULL,
    account VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (reset_token),
    KEY idx_password_reset_session_account (account_id, created_at),
    KEY idx_password_reset_session_expiry (expires_at, consumed_at),
    CONSTRAINT fk_password_reset_session_user
        FOREIGN KEY (account_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS password_reset_sms_code (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reset_token VARCHAR(64) NOT NULL,
    account_id BIGINT NOT NULL,
    account VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    next_allowed_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_password_reset_sms_token_phone (reset_token, phone, consumed_at),
    KEY idx_password_reset_sms_account (account_id, created_at),
    CONSTRAINT fk_password_reset_sms_session
        FOREIGN KEY (reset_token) REFERENCES password_reset_session(reset_token) ON DELETE CASCADE,
    CONSTRAINT fk_password_reset_sms_user
        FOREIGN KEY (account_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_account_operation_logs (
    log_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    detail VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (log_id),
    KEY idx_agent_operation_user (user_id),
    KEY idx_agent_operation_created (created_at),
    CONSTRAINT fk_agent_operation_user
        FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS forbidden_word (
    id BIGINT NOT NULL AUTO_INCREMENT,
    word VARCHAR(255) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_forbidden_word_platform (word, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    action VARCHAR(32) NOT NULL,
    operator VARCHAR(64) NOT NULL,
    operator_ip VARCHAR(64) NOT NULL,
    target_word VARCHAR(255) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    operation_time DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_operation_time (operation_time),
    KEY idx_operation_operator (operator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hit_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor VARCHAR(64) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    hit_word VARCHAR(255) NOT NULL,
    action VARCHAR(32) NOT NULL,
    action_time DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_hit_actor_time (actor, action_time),
    KEY idx_hit_time (action_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    title VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_conversation_id (conversation_id),
    KEY idx_customer_id (customer_id),
    KEY idx_conversation_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    customer_type VARCHAR(32) NULL,
    intent_recognition TEXT NULL,
    reply_strategy TEXT NULL,
    recommended_script TEXT NULL,
    hook_guidance TEXT NULL,
    success_close TEXT NULL,
    risk_warning VARCHAR(512) NULL,
    risk_suggestion VARCHAR(512) NULL,
    total_tokens INT NOT NULL DEFAULT 0,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_chat_conversation_id (conversation_id),
    KEY idx_chat_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS favorite_script (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NULL,
    platform VARCHAR(32) NOT NULL,
    customer_type VARCHAR(32) NULL,
    script_step VARCHAR(32) NOT NULL,
    script_content TEXT NOT NULL,
    script_hash VARCHAR(64) NOT NULL,
    question TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_favorite_customer_id (customer_id),
    KEY idx_script_hash (script_hash),
    KEY idx_favorite_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS token_usage_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NULL,
    usage_date DATE NOT NULL,
    total_tokens INT NOT NULL,
    prompt_tokens INT NOT NULL,
    completion_tokens INT NOT NULL,
    model_name VARCHAR(64) NULL,
    request_type VARCHAR(32) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_token_customer_id (customer_id),
    KEY idx_usage_date (usage_date),
    KEY idx_token_created_at (created_at),
    KEY idx_token_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS script_favorite (
    id BINARY(16) NOT NULL,
    staff_id VARCHAR(64) NOT NULL,
    source_talk_id VARCHAR(128) NULL,
    content_hash VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    scenario VARCHAR(100) NOT NULL,
    generated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    last_used_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_script_favorite_staff (staff_id),
    KEY idx_script_favorite_staff_source (staff_id, source_talk_id),
    KEY idx_script_favorite_staff_hash (staff_id, content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS script_favorite_tag (
    favorite_id BINARY(16) NOT NULL,
    tag VARCHAR(5) NOT NULL,
    PRIMARY KEY (favorite_id, tag),
    KEY idx_script_favorite_tag (tag),
    CONSTRAINT fk_script_favorite_tag
        FOREIGN KEY (favorite_id) REFERENCES script_favorite(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customer_token_quota (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(128) NOT NULL,
    daily_token_limit BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    updated_by VARCHAR(128) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_token_quota_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS token_usage_event (
    idempotency_key VARCHAR(160) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    conversation_id VARCHAR(160) NULL,
    request_id VARCHAR(160) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(160) NOT NULL,
    input_content_length BIGINT NOT NULL DEFAULT 0,
    output_content_length BIGINT NOT NULL DEFAULT 0,
    input_tokens BIGINT NOT NULL,
    output_tokens BIGINT NOT NULL,
    cached_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL,
    response_time_ms BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    provider_reported_cost DECIMAL(24, 10) NOT NULL DEFAULT 0,
    cost_currency VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (idempotency_key),
    KEY idx_token_usage_user_request (user_id, request_id),
    KEY idx_token_usage_user_time (user_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

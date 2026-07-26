CREATE DATABASE IF NOT EXISTS xm4
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE xm4;

-- 复用登录模块客服账号表。account 是客服手机号，也是唯一登录账号。
CREATE TABLE IF NOT EXISTS customer_service_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '客服ID',
    account VARCHAR(11) NOT NULL COMMENT '客服手机号，唯一登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希，禁止保存明文',
    display_name VARCHAR(64) NOT NULL COMMENT '客服显示名称',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
        COMMENT '账号状态：ACTIVE、DISABLED',
    last_login_at DATETIME(6) NULL COMMENT '最近登录时间',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6)
        COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_service_user_account (account),
    KEY idx_customer_service_user_status (status),
    CONSTRAINT chk_customer_service_user_phone
        CHECK (REGEXP_LIKE(account, '^1[3-9][0-9]{9}$')),
    CONSTRAINT chk_customer_service_user_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='客服登录账号表';

CREATE TABLE IF NOT EXISTS password_reset_sms_code (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '短信验证码记录ID',
    reset_token VARCHAR(64) NOT NULL COMMENT '本次找回密码流程临时凭证',
    account_id BIGINT NOT NULL COMMENT '客服账号ID',
    account VARCHAR(11) NOT NULL COMMENT '客服登录账号',
    phone VARCHAR(11) NOT NULL COMMENT '本次输入的绑定手机号',
    code_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt验证码哈希，禁止保存明文验证码',
    expires_at DATETIME(6) NOT NULL COMMENT '验证码过期时间',
    next_allowed_at DATETIME(6) NOT NULL COMMENT '下一次允许发送时间',
    consumed_at DATETIME(6) NULL COMMENT '验证码使用或失效时间',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_password_reset_token_phone (reset_token, phone, consumed_at, id DESC),
    KEY idx_password_reset_account_created (account_id, created_at DESC),
    KEY idx_password_reset_expired (expires_at, consumed_at),
    CONSTRAINT fk_password_reset_account
        FOREIGN KEY (account_id) REFERENCES customer_service_user(id),
    CONSTRAINT chk_password_reset_account_phone
        CHECK (REGEXP_LIKE(account, '^1[3-9][0-9]{9}$')
            AND REGEXP_LIKE(phone, '^1[3-9][0-9]{9}$')),
    CONSTRAINT chk_password_reset_time
        CHECK (next_allowed_at <= expires_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='找回密码短信验证码表';

CREATE TABLE IF NOT EXISTS password_reset_session (
    reset_token VARCHAR(64) NOT NULL COMMENT '找回密码流程临时凭证',
    account_id BIGINT NOT NULL COMMENT '客服账号ID',
    account VARCHAR(11) NOT NULL COMMENT '客服登录账号',
    expires_at DATETIME(6) NOT NULL COMMENT '凭证过期时间',
    consumed_at DATETIME(6) NULL COMMENT '凭证使用或失效时间',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (reset_token),
    KEY idx_password_reset_session_account (account_id, created_at DESC),
    KEY idx_password_reset_session_expired (expires_at, consumed_at),
    CONSTRAINT fk_password_reset_session_account
        FOREIGN KEY (account_id) REFERENCES customer_service_user(id),
    CONSTRAINT chk_password_reset_session_account
        CHECK (REGEXP_LIKE(account, '^1[3-9][0-9]{9}$'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='找回密码临时凭证表';

-- 找回密码：校验账号是否存在且可用。
SELECT id, account, password_hash, status
FROM customer_service_user
WHERE account = ?
LIMIT 1;

-- 账号校验成功后保存找回密码临时凭证。
INSERT INTO password_reset_session
    (reset_token, account_id, account, expires_at)
VALUES
    (?, ?, ?, ?);

-- 后续发送验证码、提交重置时校验凭证是否仍有效。
SELECT reset_token, account_id, account, expires_at
FROM password_reset_session
WHERE reset_token = ?
  AND consumed_at IS NULL
  AND expires_at >= ?
LIMIT 1;

-- 找回密码：校验输入手机号是否属于当前账号。
SELECT id, account, password_hash, status
FROM customer_service_user
WHERE account = ?
LIMIT 1;

-- 发送验证码前读取最近一条未使用验证码，用于 60 秒频控。
SELECT id, code_hash, expires_at, next_allowed_at
FROM password_reset_sms_code
WHERE reset_token = ?
  AND phone = ?
  AND consumed_at IS NULL
ORDER BY id DESC
LIMIT 1;

-- 保存验证码哈希。
INSERT INTO password_reset_sms_code
    (reset_token, account_id, account, phone, code_hash, expires_at, next_allowed_at)
VALUES
    (?, ?, ?, ?, ?, ?, ?);

-- 验证码验证成功后标记已使用。
UPDATE password_reset_sms_code
SET consumed_at = CURRENT_TIMESTAMP(6)
WHERE id = ? AND consumed_at IS NULL;

-- 定期清理或标记过期验证码。
UPDATE password_reset_sms_code
SET consumed_at = CURRENT_TIMESTAMP(6)
WHERE consumed_at IS NULL
  AND expires_at < ?;

-- 密码重置成功后更新 BCrypt 密码哈希。
UPDATE customer_service_user
SET password_hash = ?,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE id = ?;

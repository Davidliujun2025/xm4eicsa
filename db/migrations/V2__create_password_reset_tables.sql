-- 找回密码共享数据库迁移。
-- 前置条件：V1__create_auth_tables.sql 已创建 customer_service_user。
-- 本脚本由连接配置选择 xm4 数据库，因此不包含 CREATE DATABASE 或 USE。

CREATE TABLE password_reset_session (
    reset_token VARCHAR(64) NOT NULL COMMENT '本次找回密码流程的随机凭证',
    account_id BIGINT NOT NULL COMMENT '关联的客服账号ID',
    account VARCHAR(32) NOT NULL COMMENT '客服登录账号快照',
    expires_at TIMESTAMP(6) NOT NULL COMMENT '凭证过期时间',
    consumed_at TIMESTAMP(6) NULL COMMENT '凭证使用或失效时间',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (reset_token),
    KEY idx_password_reset_session_account (account_id, created_at DESC),
    KEY idx_password_reset_session_expiry (expires_at, consumed_at),
    CONSTRAINT fk_password_reset_session_user
        FOREIGN KEY (account_id) REFERENCES customer_service_user(id),
    CONSTRAINT chk_password_reset_session_expiry
        CHECK (expires_at > created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='找回密码临时凭证';

CREATE TABLE password_reset_sms_code (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '短信验证码记录ID',
    reset_token VARCHAR(64) NOT NULL COMMENT '对应的找回密码流程凭证',
    account_id BIGINT NOT NULL COMMENT '关联的客服账号ID',
    account VARCHAR(32) NOT NULL COMMENT '客服登录账号快照',
    phone VARCHAR(32) NOT NULL COMMENT '接收验证码的绑定手机号',
    code_hash VARCHAR(100) NOT NULL COMMENT '短信验证码哈希，禁止保存明文',
    expires_at TIMESTAMP(6) NOT NULL COMMENT '验证码过期时间',
    next_allowed_at TIMESTAMP(6) NOT NULL COMMENT '下一次允许发送验证码的时间',
    consumed_at TIMESTAMP(6) NULL COMMENT '验证码使用或失效时间',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_password_reset_sms_token_phone
        (reset_token, phone, consumed_at, id DESC),
    KEY idx_password_reset_sms_account
        (account_id, created_at DESC),
    KEY idx_password_reset_sms_expiry
        (expires_at, consumed_at),
    CONSTRAINT fk_password_reset_sms_session
        FOREIGN KEY (reset_token)
        REFERENCES password_reset_session(reset_token)
        ON DELETE CASCADE,
    CONSTRAINT fk_password_reset_sms_user
        FOREIGN KEY (account_id)
        REFERENCES customer_service_user(id),
    CONSTRAINT chk_password_reset_sms_phone
        CHECK (REGEXP_LIKE(phone, '^1[3-9][0-9]{9}$')),
    CONSTRAINT chk_password_reset_sms_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT chk_password_reset_sms_cooldown
        CHECK (next_allowed_at > created_at AND next_allowed_at <= expires_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='找回密码短信验证码记录';

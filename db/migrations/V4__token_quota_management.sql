-- Token 额度管理共享迁移脚本
-- 依赖：
-- 1. 登录模块已提供 customer_service_user。
-- 2. 个人 Token 消耗查看模块已提供 token_usage_event。
-- 本脚本仅创建/补齐额度管理需要的额度表和调整日志表。

CREATE TABLE IF NOT EXISTS customer_token_quota (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id VARCHAR(128) NOT NULL COMMENT '客服用户ID，建议与 customer_service_user.account 或登录态用户ID保持一致',
    daily_token_limit BIGINT NOT NULL DEFAULT 0 COMMENT '每日Token额度，0表示未配置可用额度',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE或DISABLED',
    updated_by VARCHAR(128) NULL COMMENT '最后修改人ID',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_token_quota_user (user_id),
    CONSTRAINT chk_customer_token_quota_limit CHECK (daily_token_limit >= 0),
    CONSTRAINT chk_customer_token_quota_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='客服每日Token额度表';

CREATE TABLE IF NOT EXISTS token_quota_adjustment_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    quota_id BIGINT NULL COMMENT '关联 customer_token_quota.id',
    user_id VARCHAR(128) NOT NULL COMMENT '客服用户ID快照',
    account_no VARCHAR(64) NOT NULL COMMENT '客服账号快照',
    account_name VARCHAR(64) NOT NULL COMMENT '客服名称快照',
    before_daily_token_limit BIGINT NOT NULL COMMENT '调整前每日Token额度',
    after_daily_token_limit BIGINT NOT NULL COMMENT '调整后每日Token额度',
    operator_id VARCHAR(128) NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) NOT NULL COMMENT '操作人姓名',
    reason VARCHAR(255) NOT NULL COMMENT '调整原因',
    adjusted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '调整时间',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '日志创建时间',
    PRIMARY KEY (id),
    KEY idx_token_quota_log_account_time (account_no, adjusted_at),
    KEY idx_token_quota_log_user_time (user_id, adjusted_at),
    KEY idx_token_quota_log_adjusted_at (adjusted_at),
    CONSTRAINT chk_token_quota_log_before_limit CHECK (before_daily_token_limit >= 0),
    CONSTRAINT chk_token_quota_log_after_limit CHECK (after_daily_token_limit >= 0),
    CONSTRAINT fk_token_quota_log_quota
        FOREIGN KEY (quota_id) REFERENCES customer_token_quota(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Token额度调整日志表';

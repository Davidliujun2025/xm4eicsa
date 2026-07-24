-- ============================================================
-- CarePilot AI 客服登录模块
-- 数据库：MySQL 8.0+
-- 字符集：utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS ai_customer_service
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ai_customer_service;

-- ------------------------------------------------------------
-- 1. 客服账号表
-- 手机号是唯一登录账号，密码只保存 BCrypt 哈希。
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 2. 登录页面配置表
-- 当前只允许一条配置，主键固定为 1。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS login_page_config (
    id BIGINT NOT NULL COMMENT '固定为1',
    brand_name VARCHAR(100) NOT NULL COMMENT '品牌名称',
    logo_url VARCHAR(500) NOT NULL COMMENT '品牌Logo地址',
    promo_copy VARCHAR(500) NOT NULL COMMENT '登录页宣传文案',
    forgot_password_path VARCHAR(255) NOT NULL DEFAULT '/forgot-password'
        COMMENT '找回密码页面地址',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6)
        COMMENT '更新时间',
    PRIMARY KEY (id),
    CONSTRAINT chk_login_page_config_singleton CHECK (id = 1)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='登录页面配置表';

-- 初始化或更新登录页面配置。
INSERT INTO login_page_config (
    id,
    brand_name,
    logo_url,
    promo_copy,
    forgot_password_path
) VALUES (
    1,
    'CarePilot',
    '/logo.png',
    '让每一位客服都拥有AI超能力',
    '/forgot-password'
)
ON DUPLICATE KEY UPDATE
    brand_name = VALUES(brand_name),
    logo_url = VALUES(logo_url),
    promo_copy = VALUES(promo_copy),
    forgot_password_path = VALUES(forgot_password_path);

-- ------------------------------------------------------------
-- 3. 导入预设客服账号
-- password_hash 必须由后端 BCryptPasswordEncoder(12) 生成。
-- 不允许将明文初始密码直接填写到本 SQL。
-- ------------------------------------------------------------

-- 单个账号导入模板：
--
-- INSERT INTO customer_service_user (
--     account,
--     password_hash,
--     display_name,
--     status
-- ) VALUES (
--     '13800138000',
--     '$2a$12$请替换为后端生成的BCrypt哈希',
--     '客服一号',
--     'ACTIVE'
-- )
-- ON DUPLICATE KEY UPDATE
--     password_hash = VALUES(password_hash),
--     display_name = VALUES(display_name),
--     status = VALUES(status);

-- 批量导入模板：
--
-- INSERT INTO customer_service_user (
--     account,
--     password_hash,
--     display_name,
--     status
-- ) VALUES
--     ('13800138000', '$2a$12$账号一的BCrypt哈希', '客服一号', 'ACTIVE'),
--     ('13900139000', '$2a$12$账号二的BCrypt哈希', '客服二号', 'ACTIVE')
-- ON DUPLICATE KEY UPDATE
--     password_hash = VALUES(password_hash),
--     display_name = VALUES(display_name),
--     status = VALUES(status);

-- ------------------------------------------------------------
-- 4. 登录时使用的查询
-- 后端查询到账号后，再使用 BCrypt 校验输入密码。
-- ------------------------------------------------------------

-- 按手机号查询客服账号：
-- SELECT
--     id,
--     account,
--     password_hash,
--     display_name,
--     status
-- FROM customer_service_user
-- WHERE account = ?
-- LIMIT 1;

-- 登录成功后更新最近登录时间：
-- UPDATE customer_service_user
-- SET last_login_at = CURRENT_TIMESTAMP(6)
-- WHERE id = ?;

-- 查询登录页面配置：
-- SELECT
--     brand_name,
--     logo_url,
--     promo_copy,
--     forgot_password_path
-- FROM login_page_config
-- WHERE id = 1;

-- ------------------------------------------------------------
-- 5. 管理员常用操作
-- ------------------------------------------------------------

-- 禁用客服账号：
-- UPDATE customer_service_user
-- SET status = 'DISABLED'
-- WHERE account = '13800138000';

-- 启用客服账号：
-- UPDATE customer_service_user
-- SET status = 'ACTIVE'
-- WHERE account = '13800138000';

-- 删除不在预设表中的账号前，应先备份并确认具体手机号：
-- DELETE FROM customer_service_user
-- WHERE account = '待删除手机号';

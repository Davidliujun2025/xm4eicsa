-- Compatibility migration for login module.
-- Adds canonical sys_user while preserving existing customer_service_user.

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nick_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    role_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT chk_sys_user_status CHECK (status IN ('ENABLED', 'DISABLED', 'LOCKED')),
    CONSTRAINT chk_sys_user_role_type CHECK (role_type IN ('ADMIN', 'CUSTOMER_SERVICE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sys_user (id, username, password_hash, nick_name, status, role_type, created_at, updated_at)
SELECT
    c.id,
    c.account,
    c.password_hash,
    c.display_name,
    CASE WHEN c.status = 'DISABLED' THEN 'DISABLED' ELSE 'ENABLED' END,
    'CUSTOMER_SERVICE',
    c.created_at,
    COALESCE(c.updated_at, c.created_at)
FROM customer_service_user c
WHERE c.account IS NOT NULL
  AND c.account <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user s
      WHERE s.username = c.account
  );
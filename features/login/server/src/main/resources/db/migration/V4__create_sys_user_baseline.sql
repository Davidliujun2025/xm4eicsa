-- Compatibility migration for login module.
-- Adds canonical sys_user while preserving existing customer_service_user.

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(128) NULL,
    password_hash VARCHAR(100) NOT NULL,
    password VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    role_type VARCHAR(32) NOT NULL,
    last_login_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT uk_sys_user_phone UNIQUE (phone),
    CONSTRAINT uk_sys_user_email UNIQUE (email),
    CONSTRAINT chk_sys_user_status CHECK (status IN ('ENABLED', 'DISABLED', 'LOCKED')),
    CONSTRAINT chk_sys_user_role_type CHECK (role_type IN ('ADMIN', 'CUSTOMER_SERVICE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sys_user (id, username, phone, email, password_hash, password, status, role_type, last_login_at, created_at, updated_at)
SELECT
    c.id,
    COALESCE(NULLIF(c.display_name, ''), c.account),
    NULL,
    CASE WHEN c.account LIKE '%@%' THEN c.account ELSE NULL END,
    c.password_hash,
    NULL,
    CASE WHEN c.status = 'DISABLED' THEN 'DISABLED' ELSE 'ENABLED' END,
    'CUSTOMER_SERVICE',
    c.last_login_at,
    c.created_at,
    COALESCE(c.updated_at, c.created_at)
FROM customer_service_user c
WHERE COALESCE(NULLIF(c.display_name, ''), c.account) IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user s
      WHERE s.id = c.id
         OR s.username = COALESCE(NULLIF(c.display_name, ''), c.account)
  );

CREATE TABLE customer_service_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_service_user_account UNIQUE (account),
    CONSTRAINT chk_customer_service_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE login_page_config (
    id BIGINT NOT NULL,
    brand_name VARCHAR(100) NOT NULL,
    logo_url VARCHAR(500) NOT NULL,
    promo_copy VARCHAR(500) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_login_page_config_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO login_page_config (id, brand_name, logo_url, promo_copy)
VALUES (1, '品牌名称', '/assets/brand-logo.svg', 'AI智能客服，快速响应每一位买家');

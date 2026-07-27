CREATE TABLE agent_account_operation_logs (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    detail VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_account_operation_logs_user
        FOREIGN KEY (user_id) REFERENCES agent_accounts(user_id)
        ON DELETE CASCADE,
    INDEX idx_agent_account_operation_logs_user_id (user_id),
    INDEX idx_agent_account_operation_logs_created_at (created_at)
);

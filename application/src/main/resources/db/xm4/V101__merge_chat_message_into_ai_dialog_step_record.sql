CREATE TABLE IF NOT EXISTS ai_dialog_step_record (
    record_id BIGINT NOT NULL AUTO_INCREMENT,
    session_task_id BIGINT NOT NULL,
    platform_id BIGINT NULL,
    customer_dialog TEXT NOT NULL,
    dialog_round INT NOT NULL DEFAULT 1,
    step_no TINYINT NOT NULL,
    step_round INT NOT NULL DEFAULT 1,
    ai_content TEXT NULL,
    extra_json JSON NULL,
    is_manual_edit TINYINT NOT NULL DEFAULT 0,
    is_effective TINYINT NOT NULL DEFAULT 0,
    trigger_at DATETIME(3) NULL,
    llm_reply_at DATETIME(3) NULL,
    generate_status TINYINT NOT NULL DEFAULT 0,
    fail_msg VARCHAR(255) NULL,
    operator_id BIGINT NULL,
    trace_id VARCHAR(128) NULL,
    is_delete TINYINT NOT NULL DEFAULT 0,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_task_dialog_step_round (session_task_id, dialog_round, step_no, step_round),
    KEY idx_session_task (session_task_id),
    KEY idx_task_effective (session_task_id, dialog_round, is_effective, is_delete, step_no),
    KEY idx_operator_trigger (operator_id, trigger_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'ai_dialog_step_record' AND column_name = 'dialog_round'),
    'SELECT 1',
    'ALTER TABLE ai_dialog_step_record ADD COLUMN dialog_round INT NOT NULL DEFAULT 1 AFTER customer_dialog'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'ai_dialog_step_record' AND column_name = 'prompt_tokens'),
    'SELECT 1',
    'ALTER TABLE ai_dialog_step_record ADD COLUMN prompt_tokens INT NOT NULL DEFAULT 0 AFTER is_delete'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'ai_dialog_step_record' AND column_name = 'completion_tokens'),
    'SELECT 1',
    'ALTER TABLE ai_dialog_step_record ADD COLUMN completion_tokens INT NOT NULL DEFAULT 0 AFTER prompt_tokens'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'ai_dialog_step_record' AND column_name = 'total_tokens'),
    'SELECT 1',
    'ALTER TABLE ai_dialog_step_record ADD COLUMN total_tokens INT NOT NULL DEFAULT 0 AFTER completion_tokens'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'ai_dialog_step_record'
             AND index_name = 'uk_session_step_effect'),
    'ALTER TABLE ai_dialog_step_record DROP INDEX uk_session_step_effect',
    'SELECT 1'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'ai_dialog_step_record'
             AND index_name = 'uk_task_dialog_step_round'),
    'SELECT 1',
    'ALTER TABLE ai_dialog_step_record ADD UNIQUE KEY uk_task_dialog_step_round (session_task_id, dialog_round, step_no, step_round)'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'ai_dialog_step_record'
             AND index_name = 'idx_task_effective'),
    'SELECT 1',
    'ALTER TABLE ai_dialog_step_record ADD KEY idx_task_effective (session_task_id, dialog_round, is_effective, is_delete, step_no)'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'ai_dialog_step_record'
             AND index_name = 'idx_operator_trigger'),
    'SELECT 1',
    'ALTER TABLE ai_dialog_step_record ADD KEY idx_operator_trigger (operator_id, trigger_at)'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @fallback_operator_id = (
    SELECT MIN(id)
    FROM sys_user
    WHERE role_type = 'CUSTOMER_SERVICE' AND status = 'ENABLED'
);

UPDATE conversation conversation_row
LEFT JOIN sys_user existing_user
       ON existing_user.id = CAST(conversation_row.customer_id AS UNSIGNED)
SET conversation_row.customer_id = CAST(@fallback_operator_id AS CHAR)
WHERE existing_user.id IS NULL
  AND @fallback_operator_id IS NOT NULL;

INSERT INTO ai_dialog_step_record (
    session_task_id,
    platform_id,
    customer_dialog,
    dialog_round,
    step_no,
    step_round,
    ai_content,
    extra_json,
    is_manual_edit,
    is_effective,
    trigger_at,
    llm_reply_at,
    generate_status,
    operator_id,
    trace_id,
    is_delete,
    prompt_tokens,
    completion_tokens,
    total_tokens,
    create_time,
    update_time
)
SELECT
    migrated.session_task_id,
    migrated.platform_id,
    migrated.question,
    migrated.dialog_round,
    steps.step_no,
    1,
    CASE steps.step_no
        WHEN 1 THEN migrated.intent_recognition
        WHEN 2 THEN migrated.reply_strategy
        WHEN 3 THEN migrated.recommended_script
        WHEN 4 THEN migrated.hook_guidance
        WHEN 5 THEN migrated.success_close
    END,
    JSON_OBJECT(
        'customer_type', migrated.customer_type,
        'risk_warning', migrated.risk_warning,
        'risk_suggestion', migrated.risk_suggestion,
        'legacy_chat_message_id', migrated.message_id
    ),
    0,
    1,
    migrated.created_at,
    migrated.created_at,
    1,
    migrated.operator_id,
    CONCAT('legacy-chat-message-', migrated.message_id),
    0,
    CASE WHEN steps.step_no = 1 THEN migrated.prompt_tokens ELSE 0 END,
    CASE WHEN steps.step_no = 1 THEN migrated.completion_tokens ELSE 0 END,
    CASE WHEN steps.step_no = 1 THEN migrated.total_tokens ELSE 0 END,
    migrated.created_at,
    migrated.created_at
FROM (
    SELECT
        message_row.id AS message_id,
        conversation_row.id AS session_task_id,
        CASE conversation_row.platform
            WHEN '淘宝' THEN 1
            WHEN '天猫' THEN 2
            WHEN '京东' THEN 3
            WHEN '拼多多' THEN 4
            WHEN '抖音' THEN 5
            WHEN '小红书' THEN 6
            WHEN '快手' THEN 7
            WHEN '视频号' THEN 8
            WHEN '微信小店' THEN 9
            ELSE 10
        END AS platform_id,
        CAST(conversation_row.customer_id AS UNSIGNED) AS operator_id,
        message_row.question,
        message_row.customer_type,
        message_row.intent_recognition,
        message_row.reply_strategy,
        message_row.recommended_script,
        message_row.hook_guidance,
        message_row.success_close,
        message_row.risk_warning,
        message_row.risk_suggestion,
        message_row.prompt_tokens,
        message_row.completion_tokens,
        message_row.total_tokens,
        message_row.created_at,
        ROW_NUMBER() OVER (
            PARTITION BY conversation_row.id
            ORDER BY message_row.created_at, message_row.id
        ) AS dialog_round
    FROM chat_message message_row
    JOIN conversation conversation_row
      ON conversation_row.conversation_id = message_row.conversation_id
) migrated
CROSS JOIN (
    SELECT 1 AS step_no
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
) steps
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_dialog_step_record existing_record
    WHERE existing_record.trace_id = CONCAT('legacy-chat-message-', migrated.message_id)
      AND existing_record.step_no = steps.step_no
);

DROP TABLE chat_message;

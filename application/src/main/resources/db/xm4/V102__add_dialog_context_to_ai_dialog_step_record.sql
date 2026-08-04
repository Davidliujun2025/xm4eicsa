SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'ai_dialog_step_record' AND column_name = 'dialog_context'),
    'SELECT 1',
    'ALTER TABLE ai_dialog_step_record ADD COLUMN dialog_context TEXT NULL AFTER customer_dialog'
);
PREPARE migration_statement FROM @ddl;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET SESSION group_concat_max_len = 1024 * 1024;

UPDATE ai_dialog_step_record target
JOIN (
    SELECT
        base.record_id,
        CONCAT(
            '用户问题历史：\n',
            COALESCE((
                SELECT GROUP_CONCAT(CONCAT('第', question_history.dialog_round, '轮：', question_history.customer_dialog)
                                    ORDER BY question_history.dialog_round SEPARATOR '\n')
                FROM (
                    SELECT dialog_round, MIN(customer_dialog) AS customer_dialog
                    FROM ai_dialog_step_record
                    WHERE session_task_id = base.session_task_id
                      AND is_delete = 0
                      AND is_effective = 1
                      AND dialog_round <= base.dialog_round
                    GROUP BY dialog_round
                ) question_history
            ), '无'),
            '\n\n',
            CASE base.step_no
                WHEN 1 THEN '意图识别'
                WHEN 2 THEN '回复策略'
                WHEN 3 THEN '推荐话术'
                WHEN 4 THEN '钩子引导'
                WHEN 5 THEN '成功收尾'
                ELSE '步骤'
            END,
            '历史：\n',
            COALESCE((
                SELECT GROUP_CONCAT(CONCAT('第', step_history.dialog_round, '轮：', step_history.ai_content)
                                    ORDER BY step_history.dialog_round SEPARATOR '\n')
                FROM (
                    SELECT dialog_round, ai_content
                    FROM ai_dialog_step_record
                    WHERE session_task_id = base.session_task_id
                      AND is_delete = 0
                      AND is_effective = 1
                      AND step_no = base.step_no
                      AND dialog_round <= base.dialog_round
                      AND ai_content IS NOT NULL
                      AND ai_content <> ''
                ) step_history
            ), '无')
        ) AS dialog_context
    FROM ai_dialog_step_record base
) prepared_context
  ON prepared_context.record_id = target.record_id
SET target.dialog_context = prepared_context.dialog_context
WHERE target.dialog_context IS NULL OR target.dialog_context = '';

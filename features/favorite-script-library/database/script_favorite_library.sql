CREATE DATABASE IF NOT EXISTS ai_customer_service
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ai_customer_service;

-- ------------------------------------------------------------
-- 1. 个人话术库配置表
-- 当前使用单例配置，控制每个客服最多可收藏的话术数量。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS script_library_config (
    id                 BIGINT NOT NULL COMMENT '固定为1',
    max_favorite_count INT NOT NULL DEFAULT 200 COMMENT '每个客服最多收藏的话术数量',
    updated_by         VARCHAR(128) NULL COMMENT '最后修改人ID',
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    updated_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                       ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (id),
    CONSTRAINT chk_script_library_config_singleton CHECK (id = 1),
    CONSTRAINT chk_script_library_config_max_count CHECK (max_favorite_count > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='个人话术库容量配置表';

-- ------------------------------------------------------------
-- 2. 收藏话术主表
-- 保存 AI 工作台生成的话术完整文本、适用场景、生成时间和使用排序字段。
-- user_id 用于个人库数据隔离。
-- source_talk_id 优先识别 AI 工作台生成的同一条话术。
-- content_hash 用于 source_talk_id 暂缺时兜底识别重复话术。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS script_favorite (
    id             BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏话术ID',
    user_id        VARCHAR(128) NOT NULL COMMENT '客服用户ID',
    source_talk_id VARCHAR(160) NULL COMMENT 'AI工作台生成的话术ID',
    content_hash   CHAR(64) NOT NULL COMMENT '话术完整文本SHA-256哈希',
    content        TEXT NOT NULL COMMENT '话术完整文本',
    scenario       VARCHAR(100) NOT NULL COMMENT '适用场景',
    generated_at   DATETIME(6) NOT NULL COMMENT 'AI话术生成时间，UTC',
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '收藏时间，UTC',
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                   ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    last_used_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '最近使用时间，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_script_favorite_user_source (user_id, source_talk_id),
    UNIQUE KEY uk_script_favorite_user_hash (user_id, content_hash),
    KEY idx_script_favorite_user_used (user_id, last_used_at DESC, id DESC),
    KEY idx_script_favorite_user_created (user_id, created_at DESC, id DESC),
    CONSTRAINT chk_script_favorite_content_hash
        CHECK (REGEXP_LIKE(content_hash, '^[0-9a-f]{64}$')),
    CONSTRAINT chk_script_favorite_content
        CHECK (CHAR_LENGTH(TRIM(content)) > 0),
    CONSTRAINT chk_script_favorite_scenario
        CHECK (CHAR_LENGTH(TRIM(scenario)) > 0),
    CONSTRAINT chk_script_favorite_source_talk_id
        CHECK (source_talk_id IS NULL OR CHAR_LENGTH(TRIM(source_talk_id)) > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='客服个人收藏话术主表';

-- ------------------------------------------------------------
-- 3. 收藏话术标签表
-- 一条收藏话术至少需要一个标签；最小数量由应用层校验。
-- 单个标签最多5个字符，支持预置标签和手动新增标签。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS script_favorite_tag (
    favorite_id BIGINT NOT NULL COMMENT '收藏话术ID',
    tag         VARCHAR(5) NOT NULL COMMENT '标签名称，最多5个字符',
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    PRIMARY KEY (favorite_id, tag),
    KEY idx_script_favorite_tag_tag (tag, favorite_id),
    CONSTRAINT fk_script_favorite_tag_favorite
        FOREIGN KEY (favorite_id) REFERENCES script_favorite (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_script_favorite_tag_length
        CHECK (CHAR_LENGTH(TRIM(tag)) BETWEEN 1 AND 5)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='客服个人收藏话术标签表';

-- ------------------------------------------------------------
-- 初始化或调整个人话术库容量配置。
-- MySQL 8.0.19+ 推荐使用行别名写法，避免 VALUES() 写法被废弃。
-- ------------------------------------------------------------
INSERT INTO script_library_config (
    id,
    max_favorite_count,
    updated_by
) VALUES (
    1,
    200,
    'system'
) AS new
ON DUPLICATE KEY UPDATE
    max_favorite_count = new.max_favorite_count,
    updated_by = new.updated_by;

-- ------------------------------------------------------------
-- 主要操作 SQL
-- ------------------------------------------------------------

-- 查询话术库容量上限。
SELECT max_favorite_count
FROM script_library_config
WHERE id = 1;

-- 新收藏前检查个人话术库当前数量。
SELECT COUNT(*) AS favorite_count
FROM script_favorite
WHERE user_id = ?;

-- 判断同一客服是否已经收藏同一条 AI 话术。
-- source_talk_id 有值时优先使用 source_talk_id；否则使用 content_hash。
SELECT id
FROM script_favorite
WHERE user_id = ?
  AND (
      (source_talk_id IS NOT NULL AND source_talk_id = ?)
      OR content_hash = ?
  )
LIMIT 1;

-- 新增收藏话术。
INSERT INTO script_favorite (
    user_id,
    source_talk_id,
    content_hash,
    content,
    scenario,
    generated_at,
    last_used_at
) VALUES (
    ?,
    ?,
    ?,
    ?,
    ?,
    ?,
    CURRENT_TIMESTAMP(6)
);

-- 新增收藏标签；由应用层保证至少传入1个标签。
INSERT INTO script_favorite_tag (
    favorite_id,
    tag
) VALUES
    (?, ?);

-- 再次点击收藏按钮时取消收藏。
-- 标签会通过外键 ON DELETE CASCADE 自动删除。
DELETE FROM script_favorite
WHERE id = ?
  AND user_id = ?;

-- 查看个人话术库，按最近使用时间倒序分页。
SELECT
    f.id,
    f.user_id,
    f.source_talk_id,
    f.content_hash,
    f.content,
    f.scenario,
    f.generated_at,
    f.created_at,
    f.updated_at,
    f.last_used_at
FROM script_favorite f
WHERE f.user_id = ?
ORDER BY f.last_used_at DESC, f.id DESC
LIMIT ? OFFSET ?;

-- 按标签精确检索个人收藏话术，按最近使用时间倒序分页。
SELECT
    f.id,
    f.user_id,
    f.source_talk_id,
    f.content_hash,
    f.content,
    f.scenario,
    f.generated_at,
    f.created_at,
    f.updated_at,
    f.last_used_at
FROM script_favorite f
JOIN script_favorite_tag t ON t.favorite_id = f.id
WHERE f.user_id = ?
  AND t.tag = ?
ORDER BY f.last_used_at DESC, f.id DESC
LIMIT ? OFFSET ?;

-- 按关键词检索个人收藏话术。
-- keyword 匹配话术内容或标签；个人库有容量上限，因此 LIKE 可满足原型阶段简单检索。
SELECT DISTINCT
    f.id,
    f.user_id,
    f.source_talk_id,
    f.content_hash,
    f.content,
    f.scenario,
    f.generated_at,
    f.created_at,
    f.updated_at,
    f.last_used_at
FROM script_favorite f
LEFT JOIN script_favorite_tag t ON t.favorite_id = f.id
WHERE f.user_id = ?
  AND (
      f.content LIKE CONCAT('%', ?, '%')
      OR t.tag LIKE CONCAT('%', ?, '%')
  )
ORDER BY f.last_used_at DESC, f.id DESC
LIMIT ? OFFSET ?;

-- 查询某个客服的全部标签，用于前端标签选择框。
SELECT DISTINCT t.tag
FROM script_favorite_tag t
JOIN script_favorite f ON f.id = t.favorite_id
WHERE f.user_id = ?
ORDER BY t.tag;

-- 客服复用收藏话术后，刷新最近使用时间。
UPDATE script_favorite
SET last_used_at = CURRENT_TIMESTAMP(6)
WHERE id = ?
  AND user_id = ?;

-- ============================================================
-- CarePilot AI 智能客服系统 — 数据库初始化脚本
-- 模块：智能对话工作台 (XM4-11)
-- 数据库：MySQL 8.0+
-- 日期：2026-07-17
-- 版本：v1.3
-- ============================================================

-- 创建数据库（如尚未创建）
-- CREATE DATABASE IF NOT EXISTS carepilot DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- USE carepilot;

-- -----------------------------------------------------------
-- 1. 对话记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS conversation (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id     BIGINT          NOT NULL                 COMMENT '客服人员ID（关联鉴权系统用户表）',
    platform    VARCHAR(32)     NOT NULL                 COMMENT '服务平台',
    question    VARCHAR(500)    NOT NULL                 COMMENT '客户问题内容',
    category    VARCHAR(32)     DEFAULT NULL             COMMENT '问题分类',
    status      VARCHAR(16)     NOT NULL DEFAULT 'generating' COMMENT '生成状态：generating/success/failed',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_status (status),
    INDEX idx_category (category),
    CONSTRAINT chk_status CHECK (status IN ('generating', 'success', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='对话记录表';

-- -----------------------------------------------------------
-- 2. 五步法步骤内容表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS conversation_step (
    id               BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    conversation_id  BIGINT       NOT NULL                 COMMENT '关联对话ID（应用层引用，无FK约束）',
    user_id          BIGINT       NOT NULL                 COMMENT '客服人员ID（冗余，方便按用户查询步骤）',
    step_order       TINYINT      NOT NULL                 COMMENT '步骤序号：1~5',
    step_type        VARCHAR(16)  NOT NULL                 COMMENT '步骤类型',
    title            VARCHAR(64)  DEFAULT NULL             COMMENT '步骤标题',
    label            VARCHAR(32)  DEFAULT NULL             COMMENT '内容标签（客服可编辑）',
    tags             JSON         DEFAULT NULL             COMMENT '标签数组（客服可编辑）',
    section          VARCHAR(32)  DEFAULT NULL             COMMENT '内容区块名',
    body             TEXT         NOT NULL                 COMMENT '核心内容（客服可编辑）',
    risk_warning     JSON         DEFAULT NULL             COMMENT '平台风险提示（仅第3步有值）',
    status           VARCHAR(64)  DEFAULT NULL             COMMENT '步骤状态展示文本（非机器状态码）',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近编辑时间',
    PRIMARY KEY (id),
    INDEX idx_conversation (conversation_id),
    INDEX idx_conv_step (conversation_id, step_order),
    INDEX idx_step_user (user_id),
    CONSTRAINT chk_step_order CHECK (step_order BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI生成五步法步骤内容表';

-- -----------------------------------------------------------
-- 3. 话术收藏表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS favorite (
    id               BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id          BIGINT       NOT NULL                 COMMENT '客服人员ID',
    conversation_id  BIGINT       DEFAULT NULL             COMMENT '关联对话ID（冗余）',
    step_id          BIGINT       NOT NULL                 COMMENT '关联步骤ID',
    step_type        VARCHAR(16)  NOT NULL                 COMMENT '步骤类型',
    content          TEXT         NOT NULL                 COMMENT '话术内容快照',
    platform         VARCHAR(32)  NOT NULL                 COMMENT '服务平台',
    category         VARCHAR(32)  DEFAULT NULL             COMMENT '问题分类（快照）',
    question         VARCHAR(500) DEFAULT NULL             COMMENT '关联客户问题',
    collected_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_step (user_id, step_id),
    INDEX idx_user (user_id),
    INDEX idx_user_collected (user_id, collected_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话术收藏表';

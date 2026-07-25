# 找回密码 MySQL 表结构说明

## 1. 数据库说明

最终集成时使用仓库统一数据库：

```text
xm4
```

找回密码功能不单独创建客服账号表，而是复用登录模块已有的账号表：

```text
customer_service_user
```

找回密码新增两张表：

```text
password_reset_session
password_reset_sms_code
```

## 2. 复用表：customer_service_user

用途：保存客服登录账号和密码哈希。找回密码成功后只更新该表的 `password_hash` 字段。

关键字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 客服账号 ID，主键。 |
| `account` | VARCHAR(11) | 客服登录账号，当前项目中账号即手机号。 |
| `password_hash` | VARCHAR(100) | BCrypt 密码哈希，禁止保存明文密码。 |
| `display_name` | VARCHAR(64) | 客服展示名称。 |
| `status` | VARCHAR(16) | 账号状态，建议使用 `ACTIVE`、`DISABLED`。 |
| `last_login_at` | DATETIME(6) | 最近登录时间。 |
| `created_at` | DATETIME(6) | 创建时间。 |
| `updated_at` | DATETIME(6) | 更新时间。 |

查询账号：

```sql
SELECT id, account, password_hash, display_name, status
FROM customer_service_user
WHERE account = ?
LIMIT 1;
```

更新密码：

```sql
UPDATE customer_service_user
SET password_hash = ?,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE id = ?;
```

## 3. 新增表：password_reset_session

用途：保存本次找回密码流程的临时凭证。账号校验成功后生成 `resetToken`，后续获取验证码和提交重置时都需要带上该凭证。

建表 SQL：

```sql
CREATE TABLE IF NOT EXISTS password_reset_session (
    reset_token VARCHAR(64) NOT NULL COMMENT '找回密码流程临时凭证',
    account_id BIGINT NOT NULL COMMENT '客服账号ID',
    account VARCHAR(11) NOT NULL COMMENT '客服登录账号',
    expires_at DATETIME(6) NOT NULL COMMENT '凭证过期时间',
    consumed_at DATETIME(6) NULL COMMENT '凭证使用或失效时间',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (reset_token),
    KEY idx_password_reset_session_account (account_id, created_at DESC),
    KEY idx_password_reset_session_expired (expires_at, consumed_at),
    CONSTRAINT fk_password_reset_session_account
        FOREIGN KEY (account_id) REFERENCES customer_service_user(id),
    CONSTRAINT chk_password_reset_session_account
        CHECK (REGEXP_LIKE(account, '^1[3-9][0-9]{9}$'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='找回密码临时凭证表';
```

主要字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `reset_token` | VARCHAR(64) | 临时凭证，建议使用安全随机字符串。 |
| `account_id` | BIGINT | 关联 `customer_service_user.id`。 |
| `account` | VARCHAR(11) | 冗余保存账号，便于排查和审计。 |
| `expires_at` | DATETIME(6) | 凭证过期时间，建议 10 分钟。 |
| `consumed_at` | DATETIME(6) | 重置成功后写入，表示凭证已使用。 |
| `created_at` | DATETIME(6) | 创建时间。 |

保存临时凭证：

```sql
INSERT INTO password_reset_session
    (reset_token, account_id, account, expires_at)
VALUES
    (?, ?, ?, ?);
```

校验临时凭证：

```sql
SELECT reset_token, account_id, account, expires_at
FROM password_reset_session
WHERE reset_token = ?
  AND consumed_at IS NULL
  AND expires_at >= CURRENT_TIMESTAMP(6)
LIMIT 1;
```

标记凭证已使用：

```sql
UPDATE password_reset_session
SET consumed_at = CURRENT_TIMESTAMP(6)
WHERE reset_token = ?
  AND consumed_at IS NULL;
```

## 4. 新增表：password_reset_sms_code

用途：保存短信验证码发送记录、验证码哈希、过期时间和 60 秒冷却时间。

建表 SQL：

```sql
CREATE TABLE IF NOT EXISTS password_reset_sms_code (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '短信验证码记录ID',
    reset_token VARCHAR(64) NOT NULL COMMENT '本次找回密码流程临时凭证',
    account_id BIGINT NOT NULL COMMENT '客服账号ID',
    account VARCHAR(11) NOT NULL COMMENT '客服登录账号',
    phone VARCHAR(11) NOT NULL COMMENT '本次输入的绑定手机号',
    code_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt验证码哈希，禁止保存明文验证码',
    expires_at DATETIME(6) NOT NULL COMMENT '验证码过期时间',
    next_allowed_at DATETIME(6) NOT NULL COMMENT '下一次允许发送时间',
    consumed_at DATETIME(6) NULL COMMENT '验证码使用或失效时间',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_password_reset_token_phone (reset_token, phone, consumed_at, id DESC),
    KEY idx_password_reset_account_created (account_id, created_at DESC),
    KEY idx_password_reset_expired (expires_at, consumed_at),
    CONSTRAINT fk_password_reset_account
        FOREIGN KEY (account_id) REFERENCES customer_service_user(id),
    CONSTRAINT chk_password_reset_sms_account_phone
        CHECK (REGEXP_LIKE(account, '^1[3-9][0-9]{9}$')
            AND REGEXP_LIKE(phone, '^1[3-9][0-9]{9}$')),
    CONSTRAINT chk_password_reset_sms_time
        CHECK (next_allowed_at <= expires_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='找回密码短信验证码表';
```

主要字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 验证码记录主键。 |
| `reset_token` | VARCHAR(64) | 对应本次找回密码流程。 |
| `account_id` | BIGINT | 关联客服账号 ID。 |
| `account` | VARCHAR(11) | 客服账号。 |
| `phone` | VARCHAR(11) | 用户输入的绑定手机号。 |
| `code_hash` | VARCHAR(100) | 验证码哈希，禁止保存明文验证码。 |
| `expires_at` | DATETIME(6) | 验证码过期时间，建议 5 分钟。 |
| `next_allowed_at` | DATETIME(6) | 下一次允许发送时间，用于 60 秒倒计时。 |
| `consumed_at` | DATETIME(6) | 验证码验证成功或过期清理后写入。 |
| `created_at` | DATETIME(6) | 创建时间。 |

读取最近一次有效验证码：

```sql
SELECT id, code_hash, expires_at, next_allowed_at
FROM password_reset_sms_code
WHERE reset_token = ?
  AND phone = ?
  AND consumed_at IS NULL
ORDER BY id DESC
LIMIT 1;
```

保存验证码哈希：

```sql
INSERT INTO password_reset_sms_code
    (reset_token, account_id, account, phone, code_hash, expires_at, next_allowed_at)
VALUES
    (?, ?, ?, ?, ?, ?, ?);
```

验证码使用成功后标记：

```sql
UPDATE password_reset_sms_code
SET consumed_at = CURRENT_TIMESTAMP(6)
WHERE id = ?
  AND consumed_at IS NULL;
```

清理过期验证码：

```sql
UPDATE password_reset_sms_code
SET consumed_at = CURRENT_TIMESTAMP(6)
WHERE consumed_at IS NULL
  AND expires_at < CURRENT_TIMESTAMP(6);
```

## 5. 推荐迁移文件位置

功能开发阶段可以先放在：

```text
features/forgot-password/server/src/main/resources/db/migration/V1__create_password_reset_tables.sql
```

最终统一部署时，应同步到仓库共享迁移目录：

```text
db/migrations/Vx__create_password_reset_tables.sql
```

其中 `Vx` 需要根据已有迁移文件顺序递增，不能和其他模块重复。

## 6. 数据安全说明

- 明文密码不能写入数据库。
- 明文短信验证码不能写入数据库。
- `password_hash` 使用 BCrypt。
- `code_hash` 建议使用 BCrypt 或 HMAC 哈希。
- `resetToken` 必须设置过期时间，重置成功后立即失效。
- 验证码建议 5 分钟有效，获取验证码按钮 60 秒冷却。

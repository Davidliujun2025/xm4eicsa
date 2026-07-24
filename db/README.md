# Shared Database

The final integrated application should use one MySQL database:

```text
xm4
```

## Local Development Connection

Local Mac development should connect through the SSH tunnel helper:

```bash
scripts/start-mysql-tunnel.sh
```

Then connect to:

```text
127.0.0.1:3307/xm4
```

## Server Deployment Connection

When application services run on Tencent Cloud Ubuntu, connect directly to:

```text
127.0.0.1:3306/xm4
```

## Migration Directory

Place shared SQL migration scripts in:

```text
db/migrations
```

Use a monotonic filename pattern:

```text
V1__init_xm4_schema.sql
V2__create_auth_tables.sql
V3__create_chat_tables.sql
```

## Table Naming Guidance

Prefer descriptive names that avoid cross-feature collisions:

```text
auth_users
password_reset_tokens
agent_accounts
chat_sessions
chat_messages
favorite_scripts
forbidden_words
forbidden_word_hit_logs
token_quota_rules
token_usage_events
```

Feature owners should document required tables before final integration. Do not create separate databases per feature for the final deployment.
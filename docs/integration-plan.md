# Integration Plan

This repository is organized by user story under `features/`. Each developer can continue working in their own feature directory without changing other modules. The integration layer below defines how the project will share one database and one deployment entry point later.

## Goals

- Keep feature development isolated under `features/<story-name>`.
- Use one shared MySQL database named `xm4` for final integration.
- Avoid opening MySQL `3306` to the public internet.
- Use SSH tunneling for local development from Mac to Tencent Cloud MySQL.
- Use direct local MySQL access only after the app is deployed on the Tencent Cloud Ubuntu server.
- Prepare deployment files without forcing unfinished user stories to change now.

## Local Development Database Access

Local services should not connect to remote MySQL directly. Start the tunnel first:

```bash
scripts/start-mysql-tunnel.sh
```

Then local code connects to:

```text
127.0.0.1:3307/xm4
```

Recommended local environment values:

```env
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3307
MYSQL_DATABASE=xm4
MYSQL_USERNAME=root
MYSQL_PASSWORD=change_me
MYSQL_URL=jdbc:mysql://127.0.0.1:3307/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

## Tencent Cloud Deployment Database Access

When services run on the Tencent Cloud Ubuntu server, they should connect to MySQL directly through the server loopback address:

```env
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=xm4
MYSQL_USERNAME=root
MYSQL_PASSWORD=change_me
MYSQL_URL=jdbc:mysql://127.0.0.1:3306/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

## Shared API Prefixes

Use stable API prefixes so Nginx can route traffic later:

```text
/api/auth/**
/api/password/**
/api/agents/**
/api/chat/**
/api/scripts/**
/api/forbidden-words/**
/api/token-quota/**
/api/token-usage/**
```

## Developer Handoff Checklist

Each feature owner should eventually provide:

```text
Feature name:
Backend port:
API prefix:
Required tables:
Main table fields:
Needs logged-in user data: yes/no
Called by other modules: yes/no
Local startup command:
Build command:
```

## Current Non-Invasive Integration Assets

- `.env.example`: shared environment variable template.
- `scripts/start-mysql-tunnel.sh`: local SSH tunnel helper.
- `docs/service-registry.md`: current service and port registry.
- `docs/deployment-tencent-cloud.md`: deployment notes for Tencent Cloud.
- `db/README.md`: shared database migration rules.
- `deploy/nginx/xm4eicsa.conf.example`: Nginx virtual host draft.

These files do not alter feature code or local startup behavior.
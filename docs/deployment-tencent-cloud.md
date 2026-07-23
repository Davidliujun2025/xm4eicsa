# Tencent Cloud Deployment Notes

This document tracks deployment preparation for the final Tencent Cloud Ubuntu environment. It is a deployment guide, not a requirement for feature developers to change their local code immediately.

## Target Shape

```text
Browser
  -> virtual domain or public IP
  -> Nginx on Tencent Cloud Ubuntu
  -> static frontend files and /api/* reverse proxy rules
  -> feature backend services on 127.0.0.1:<port>
  -> MySQL on 127.0.0.1:3306, database xm4
```

## Local Development Versus Server Deployment

Local Mac development:

```text
Local app -> 127.0.0.1:3307 -> SSH tunnel -> Tencent Cloud 127.0.0.1:3306 -> MySQL xm4
```

Tencent Cloud deployment:

```text
Server app -> 127.0.0.1:3306 -> MySQL xm4
```

Do not open MySQL port `3306` to the public internet.

## Server Preparation Checklist

- Install JDK for Java services.
- Install Maven if Spring Boot modules are built on the server.
- Install Node.js if frontend or Node-based tools are built on the server.
- Install and configure MySQL.
- Create database `xm4`.
- Install Nginx.
- Copy static frontend build outputs into the configured web root.
- Start backend services on localhost-only ports.
- Enable Nginx reverse proxy for `/api/*` routes.
- Verify public access through the virtual domain or public IP.

## Environment Values On Server

Use server-local MySQL access for deployed services:

```env
APP_ENV=prod
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=xm4
MYSQL_USERNAME=root
MYSQL_PASSWORD=change_me
MYSQL_URL=jdbc:mysql://127.0.0.1:3306/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

## Deployment Order

1. Prepare Ubuntu runtime packages.
2. Prepare MySQL database `xm4`.
3. Apply database migration scripts from `db/migrations` when available.
4. Build and upload frontend assets.
5. Build and upload backend artifacts.
6. Start backend services and confirm they listen on localhost ports.
7. Enable Nginx configuration from `deploy/nginx`.
8. Test `/` and each `/api/*` route through the public entry point.

## Notes

- Real passwords should be stored in server environment files or shell profiles, not committed to this repository.
- Keep backend services bound to `127.0.0.1` when Nginx is the public entry point.
- Update `docs/service-registry.md` whenever a feature backend port or API prefix changes.
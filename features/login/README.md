# login

CarePilot AI 客服登录功能，包含 Vue 3 登录页面、Spring Boot 认证后端、MySQL 数据结构、Redis 会话和前后端联调配置。

## 目录结构

```text
features/login/
├─ server/       Spring Boot 后端、测试和 Docker Compose
├─ ui/           Vue 3 + TypeScript + Vite 前端
├─ schema.sql    登录数据库设计及预设账号导入模板
└─ README.md
```

## 主要功能

- 获取登录页品牌及宣传文案配置
- 客服账号密码登录
- BCrypt 密码校验
- 账号不存在、密码错误和空参数提示
- Access Token、Refresh Token 与 CSRF 防护
- Refresh Token 轮换及重放防护
- “记住我”长期会话
- 获取当前客服及退出登录
- 登录失败次数限制

## API

- `GET /api/v1/public/login-config`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`

## 启动后端

```powershell
cd features/login/server
docker compose up -d --build
```

后端地址：`http://localhost:8080`

健康检查：`http://localhost:8080/actuator/health`

## 启动前端

```powershell
cd features/login/ui
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

前端地址：`http://localhost:5173/`

## 构建与测试

```powershell
cd features/login/ui
npm run build
```

```powershell
cd features/login/server
mvn test
```

后端使用 Flyway 自动执行数据库迁移。手机号唯一账号的目标结构和预设账号导入模板见 `schema.sql`。初始密码必须先转换为 BCrypt 哈希，禁止保存明文密码。

前端已经配置跨域 Cookie、CSRF Token、后端错误码提示和登录成功跳转。

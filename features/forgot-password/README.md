# forgot-password

找回登录密码功能目录，用于承接电商智能客服 AI 助手的客服账号密码重置流程。

## 功能范围

- 登录页点击“忘记密码”后进入密码重置页面。
- 校验客服账号格式、账号是否存在、账号是否可用。
- 校验账号绑定手机号，发送 6 位短信验证码。
- 验证短信验证码、新密码规则和两次密码一致性。
- 重置 `customer_service_user.password_hash`，成功后引导用户返回登录页。

## 目录结构

```text
features/forgot-password/
├─ ui/        Vue 3 找回密码页面和前端接口封装
├─ server/    Spring Boot 接口、验证码和密码更新逻辑
├─ docs/      接口、流程和 MySQL 表结构说明
└─ README.md  模块总说明
```



## 文档

- [接口说明](docs/api.md)
- [流程说明](docs/flow.md)
- [MySQL 表结构说明](docs/mysql.md)

## 集成说明

仓库当前按用户故事拆分功能，找回密码代码优先放在 `features/forgot-password`。最终和登录模块联调时，需要复用 `features/login` 中已有的客服账号表和密码加密方式。

前后端统一 API 前缀：

```text
/api/password/**
```

推荐复用数据表：

```text
customer_service_user
```

新增数据表：

```text
password_reset_session
password_reset_sms_code
```

## 本地启动

首次使用 MySQL 时执行：

```text
server/src/main/resources/db/password_reset_mysql.sql
```

默认连接 `localhost:3306/xm4`，可通过 `MYSQL_URL`、`MYSQL_USERNAME` 和
`MYSQL_PASSWORD` 覆盖。

启动后端：

```bash
cd server
mvn test
mvn spring-boot:run
```

启动前端：

```bash
cd ui
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，并将 `/api` 请求代理到
`http://localhost:8080`。

## 构建

```bash
cd server
mvn package

cd ../ui
npm run build
```

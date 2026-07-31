# XM4EICSA 单体多模块项目

项目已按“单体部署、多模块开发”方式整合：

- 根目录 `pom.xml` 统一编排所有后端模块。
- `application` 是唯一可执行 Spring Boot 应用，监听 `8080`。
- 所有模块共享 `sys_user`、Cookie 登录态、角色权限和 Flyway 基准库。
- 生产配置使用 `ddl-auto: validate`，应用不会自动修改表结构。
- 管理员登录后进入 `/admin/users/`，客服登录后进入 `/workbench/`。

## 后端构建和启动

要求 Java 21、Maven 3.9+、MySQL 8 和 Redis。

```bash
mvn clean package

export DB_URL='jdbc:mysql://127.0.0.1:3306/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export DB_USERNAME='root'
export DB_PASSWORD='your-password'
export REDIS_HOST='127.0.0.1'
export REDIS_PORT='6379'
export JWT_SECRET_BASE64='replace-with-a-random-base64-secret'

# 仅首次部署且库中没有管理员时临时启用，创建成功后关闭：
export BOOTSTRAP_ADMIN_ENABLED='true'
export BOOTSTRAP_ADMIN_NAME='系统管理员'
export BOOTSTRAP_ADMIN_PHONE='replace-with-admin-phone'
export BOOTSTRAP_ADMIN_EMAIL='replace-with-admin-email'
export BOOTSTRAP_ADMIN_PASSWORD='replace-with-a-strong-password'

java -jar application/target/xm4eicsa-application-1.0.0-SNAPSHOT.jar
```

首次启动由 `application/src/main/resources/db/xm4` 中的 Flyway 脚本创建统一表结构。

## 前端

前端开发服务器都将 `/api` 代理到 `http://localhost:8080`。生产部署入口：

- `/`：登录
- `/forgot-password/`：找回密码
- `/workbench/`：AI 对话工作台
- `/favorite-script-library/`：个人话术库
- `/token-usage/`：Token 使用统计
- `/admin/users/`：用户管理
- `/admin/tokens/`：Token 配额管理
- `/admin/forbidden-words/`：违禁词管理

构建与腾讯云部署脚本见 `scripts/deploy-frontends-tencent.sh`，Nginx 示例见
`deploy/nginx/xm4eicsa.conf.example`。

## 数据库变更规则

`application/src/main/resources/db/xm4` 是唯一数据库结构来源。禁止在功能模块中启用
`ddl-auto=update/create`；所有结构变化必须新增 Flyway 迁移并经联调验证。

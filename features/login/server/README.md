# AI 智能客服登录后端

面向客服人员登录场景的 Spring Boot 3 后端，提供登录页品牌配置、账号密码认证、短期访问令牌、可轮换 Refresh Token、记住登录、退出登录和渐进式防暴力破解能力。

## 技术与环境

- Java 21、Maven 3.6.3+
- Spring Boot 3.5.16、Spring Security、Spring Data JPA/Redis、Flyway
- MySQL 8、Redis 7

最简启动方式是安装 Docker 后在项目根目录执行：

```bash
docker compose up --build
```

系统不会创建或预设本地开发账号。登录时直接读取现有 MySQL `sys_user` 表，可使用
`username`、`phone` 或 `email` 作为账号，并使用 `password_hash`（兼容旧数据的
`password`）校验密码。用户状态和角色分别读取 `status`、`role_type`。

生产环境必须启用 `prod` profile、不得启用 `dev` profile，并且必须修改 `JWT_SECRET_BASE64`、数据库密码、允许来源和 `SECURE_COOKIES=true`；不满足关键安全项时应用会拒绝启动。
如果服务部署在受信任的反向代理后，再按基础设施配置 `FORWARD_HEADERS_STRATEGY=native`；默认不信任客户端伪造的转发头。

## 前端调用顺序

1. 调用 `GET /api/v1/public/login-config`，展示品牌配置并取得 `XSRF-TOKEN` Cookie。
2. 对每一次登录、刷新和退出请求，都从当前 Cookie 重新读取值并放入 `X-XSRF-TOKEN` 请求头，同时启用浏览器凭证传递（例如 `credentials: 'include'`）；不要在应用内长期缓存旧 CSRF Token。
3. 调用 `POST /api/v1/auth/login`，请求体示例：

```json
{
  "account": "<sys_user.username|phone|email>",
  "password": "<该账号的密码>",
  "rememberMe": true
}
```

4. 登录成功后 CSRF Token 会轮换；再次调用配置接口取得当前 Token，再按响应中的 `data.redirectPath` 跳转。应用重新打开时同样先获取配置及 CSRF Token，再调用 `POST /api/v1/auth/refresh`，成功即可自动登录。

## API

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/v1/public/login-config` | 获取宣传文案、品牌信息、密码策略及找回密码路径 |
| POST | `/api/v1/auth/login` | 账号密码登录 |
| POST | `/api/v1/auth/refresh` | 消费并轮换 Refresh Token |
| GET | `/api/v1/auth/me` | 获取当前客服信息 |
| POST | `/api/v1/auth/logout` | 撤销当前 Refresh Token 并清除 Cookie |
| GET | `/actuator/health` | 健康检查 |

接口统一返回 `code`、`message` 和 `data`。登录失败不会返回账号、密码或令牌；浏览器中的令牌 Cookie 均为 HttpOnly。

## 品牌配置

Flyway 初始化 `login_page_config` 的唯一记录（`id=1`）。联调前应将占位的品牌名称、Logo URL 和宣传文案替换为原型中的准确内容：

```sql
UPDATE login_page_config
SET brand_name = '实际品牌名',
    logo_url = '实际 Logo URL',
    promo_copy = '实际宣传文案'
WHERE id = 1;
```

## 本机构建与测试

```bash
mvn clean test
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Docker 可用时执行包含 MySQL、Redis 的完整集成测试：

```bash
mvn -Pintegration-tests verify
```

中国大陆网络访问 Maven Central 较慢时，可使用仓库内的可选镜像配置：

```bash
mvn -s maven-settings-cn.xml clean test
```

完整基础设施集成测试在 Docker 可用时运行；没有 Docker 时，Testcontainers 场景会自动跳过，单元测试仍正常执行。

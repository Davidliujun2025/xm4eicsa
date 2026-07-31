# 单体模块注册表

所有后端模块由 `application` 聚合为一个进程，对外只监听 `8080`。

| 模块 | API |
|---|---|
| 登录与当前用户 | `/api/v1/auth/**`, `/api/v1/public/**` |
| 找回密码 | `/api/auth/password-reset/**` |
| 用户管理 | `/api/admin/customer-service-users/**` |
| Token 配额管理 | `/api/admin/token-quotas/**` |
| 违禁词与审计 | `/api/forbidden-words/**`, `/api/chat-audit/**` |
| AI 工作台 | `/api/v1/conversations/**`, `/api/v1/tokens/**` |
| 个人话术库 | `/api/v1/script-favorites/**` |
| Token 使用统计 | `/api/v1/token-usage/**` |

身份只允许来自统一登录 Cookie/JWT 解析出的 Spring Security Principal。浏览器不得通过
`X-User-Id`、`X-Customer-Id`、`X-Staff-Id` 或 `X-Operator` 自报身份。

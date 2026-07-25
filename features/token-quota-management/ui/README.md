# Token 额度管理前端

这是 Token 额度管理的 Vue 3 + Vite 前端，已经接入同级目录中的 Spring Boot 后端接口。

## 启动顺序

先启动后端：

```bash
cd ..
mvn spring-boot:run
```

后端默认端口：`8081`

再启动前端：

```bash
npm run dev
```

前端默认端口：`5173`

开发环境下，`vite.config.ts` 会把 `/api` 请求代理到 `http://localhost:8081`。

## 已接入接口

- `GET /api/admin/token-quotas/summary`
- `GET /api/admin/token-quotas/accounts`
- `PUT /api/admin/token-quotas/accounts/{accountNo}/daily-quota`
- `PUT /api/admin/token-quotas/accounts/daily-quota/batch`
- `GET /api/admin/token-quotas/adjustment-logs`

## 后续对接真实系统时优先确认

- 管理员身份是否仍由前端传 `operatorId`、`operatorName`
- 搜索条件是否只按客服账号查，还是要同时支持姓名
- 批量调整是否需要后端事务保证全部成功或全部失败
- 生产环境是否设置 `VITE_API_BASE_URL`

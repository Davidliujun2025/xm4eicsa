# CarePilot AI Token 额度管理后端骨架

这是一个与“找回密码”项目相互独立的 Spring Boot 后端程序，用于后台管理员管理客服账号每日 Token 使用额度。

项目提供两种运行模式：

- `memory`：默认模式，使用内存演示数据，适合没有数据库时本地演示
- `mysql`：连接 MySQL，复用 `customer_service_user`、`token_usage_event`、`customer_token_quota`，并写入 `token_quota_adjustment_log`

当前能力包括：

- 独立 Spring Boot 程序，默认端口 `8081`
- 推荐仓库接口前缀：`/api/token-quota`
- 兼容旧前端接口前缀：`/api/admin/token-quotas`
- 管理员查看客服 Token 配额列表
- 修改客服每日 Token 配额
- Token 使用量记录接口
- 80% 使用率标记：`Token 使用率较高`
- 达到或超过每日额度标记：`已超出建议额度`
- 额度调整日志查询
- MySQL 仓储实现，前端接口路径保持不变

## 本地启动

```bash
mvn spring-boot:run
```

默认启动的是 `memory` 模式。

默认演示账号：

| 客服账号 | 客服名称 | 每日配额 | 已使用 Token |
| --- | --- | ---: | ---: |
| CS1001 | 客服一号 | 120000 | 32000 |
| CS1002 | 客服二号 | 100000 | 82000 |
| CS1003 | 客服三号 | 80000 | 91000 |

## MySQL 启动

先在 `ai_customer_service` 数据库执行：

```text
src/main/resources/db/mysql/token_quota_schema.sql
```

然后启动 MySQL 模式：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

也可以通过环境变量覆盖数据库连接：

```bash
set TOKEN_QUOTA_DB_URL=jdbc:mysql://127.0.0.1:3307/xm4?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
set TOKEN_QUOTA_DB_USERNAME=root
set TOKEN_QUOTA_DB_PASSWORD=your_password
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

MySQL 模式下：

- 客服账号列表来自 `customer_service_user`
- 每日额度来自 `customer_token_quota`
- 已用 Token、AI 调用次数、业务数从 `token_usage_event` 按当天 UTC 时间统计
- 修改额度会写入 `customer_token_quota`，同时新增 `token_quota_adjustment_log`

## 接口 1：查看 Token 配额列表

`GET /api/token-quota/accounts?page=1&pageSize=20`

可选查询参数：

- `accountNo`：客服账号，支持模糊筛选
- `statusCode`：状态筛选，可选 `NORMAL`、`HIGH_USAGE`、`EXCEEDED_RECOMMENDED`
- `page`：页码，默认 1
- `pageSize`：每页条数，默认 20，最大 100

成功响应示例：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "records": [
      {
        "accountNo": "CS1002",
        "accountName": "客服二号",
        "dailyQuota": 100000,
        "usedTokens": 82000,
        "remainingTokens": 18000,
        "overageTokens": 0,
        "usageRatePercent": 82.0,
        "aiCallCount": 37,
        "businessCount": 16,
        "statusCode": "HIGH_USAGE",
        "statusLabel": "Token 使用率较高",
        "enabled": true
      }
    ],
    "total": 3,
    "page": 1,
    "pageSize": 20,
    "totalPages": 1
  }
}
```

## 接口 2：查看单个客服账号额度详情

`GET /api/token-quota/accounts/{accountNo}`

示例：

`GET /api/token-quota/accounts/CS1001`

## 接口 3：查看统计汇总

`GET /api/token-quota/summary`

用于前端统计卡片，返回账号总数、每日配额总量、今日已使用、总体使用率、高使用率账号数和超额账号数。

## 接口 4：修改每日 Token 配额

`PUT /api/token-quota/accounts/{accountNo}/daily-quota`

请求：

```json
{
  "dailyQuota": 150000,
  "reason": "根据近期 AI 调用量上调额度",
  "operatorId": "admin-001",
  "operatorName": "后台管理员"
}
```

成功响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "额度设置成功",
  "data": {
    "accountNo": "CS1001",
    "beforeQuota": 120000,
    "afterQuota": 150000,
    "adjustedAt": "2026-07-20T10:30:00"
  }
}
```

校验规则：

- `dailyQuota` 不能为空
- 只允许正整数
- 不得超过系统允许的最大额度，默认 `10000000`
- `reason`、`operatorId`、`operatorName` 不能为空

## 接口 5：批量修改每日 Token 配额

`PUT /api/token-quota/accounts/daily-quota/batch`

请求：

```json
{
  "accountNos": ["CS1001", "CS1002"],
  "dailyQuota": 150000,
  "reason": "统一调整额度",
  "operatorId": "admin-001",
  "operatorName": "后台管理员"
}
```

## 接口 6：记录 Token 消耗

这个接口一般由 AI 对话模块或后端调用，不一定直接暴露给前端。

`POST /api/token-quota/usage-records`

请求：

```json
{
  "accountNo": "CS1001",
  "consumedTokens": 2500,
  "aiCallCount": 1,
  "businessCount": 1
}
```

说明：

- 即使 Token 使用量达到或超过每日额度，也继续累计消耗数据
- 达到 80% 后返回 `HIGH_USAGE`
- 达到或超过每日额度后返回 `EXCEEDED_RECOMMENDED`

## 接口 7：查看额度调整记录

`GET /api/token-quota/adjustment-logs?page=1&pageSize=20`

可选查询参数：

- `accountNo`：客服账号
- `startTime`：调整开始时间，格式如 `2026-07-20T00:00:00`
- `endTime`：调整结束时间，格式如 `2026-07-20T23:59:59`
- `page`：页码
- `pageSize`：每页条数

返回字段包含：

- 客服账号
- 调整时间
- 调整前额度
- 调整后额度
- 操作人
- 调整原因

## MySQL 表

本模块主要使用这些表：

- `customer_service_user`：复用登录模块客服账号表
- `token_usage_event`：复用个人 Token 消耗查看模块的大模型调用事件表
- `customer_token_quota`：保存客服每日 Token 额度
- `token_quota_adjustment_log`：保存管理员额度调整记录

## 后续接前端时优先确认

- 页面列表字段是否与 `TokenQuotaItemResponse` 一致
- 状态码是否使用 `NORMAL`、`HIGH_USAGE`、`EXCEEDED_RECOMMENDED`
- “修改额度”弹窗是否需要传调整原因
- 操作人信息由前端传，还是从登录态/Token 中解析
- 是否需要将 Token 使用进度条颜色规则也由后端返回

## 测试

```bash
mvn test
```

已包含基础测试：

- 80% 阈值状态判断
- 达到/超过额度状态判断
- 修改额度并生成调整日志
- 超额后继续累计 Token 消耗

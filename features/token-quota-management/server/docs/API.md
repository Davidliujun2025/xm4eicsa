# API 接入说明

版本：`v1`。时间使用 ISO-8601 UTC。统计区间采用 `[from, to)`。

## 认证

- 客服查询：`X-User-Id`，仅允许可信网关注入。
- 内部上报：`X-Internal-Api-Key`。
- 详情查询始终附加当前用户条件；不存在和不属于当前用户均返回 `404`。

## 上报模型调用

`POST /api/v1/internal/token-usage/events`

```json
{
  "idempotencyKey": "provider-request-id",
  "userId": "agent-1001",
  "conversationId": "conversation-20",
  "requestId": "workbench-request-30",
  "provider": "KIMI",
  "model": "moonshot-v1",
  "inputContentLength": 420,
  "outputContentLength": 110,
  "inputTokens": 1200,
  "outputTokens": 230,
  "cachedInputTokens": 800,
  "totalTokens": 1430,
  "responseTimeMs": 860,
  "status": "SUCCEEDED",
  "occurredAt": "2026-07-18T08:00:00Z",
  "providerReportedCost": 0.0042,
  "costCurrency": "USD"
}
```

Token 必须来自厂商响应，不允许本地估算。首次写入返回 `201`；相同 `idempotencyKey` 返回 `200`、`duplicate=true`。

## 今日统计

`GET /api/v1/token-usage/me/today`

返回 Input、Output、Cached、Total Token、成功/失败次数、平均响应时间、每日额度、剩余额度、使用率和高使用率状态。无记录时 `empty=true`。

## 四周期汇总

`GET /api/v1/token-usage/me/summary`

返回 `today`、`thisWeek`、`thisMonth`、`history`。周从业务时区星期一开始，月从当月 1 日开始。

兼容自定义区间：

`GET /api/v1/token-usage/me/summary?from=2026-07-01T00:00:00Z&to=2026-08-01T00:00:00Z`

## 趋势

```text
GET /api/v1/token-usage/me/trend?range=LAST_7_DAYS
GET /api/v1/token-usage/me/trend?range=LAST_30_DAYS
GET /api/v1/token-usage/me/trend?range=CUSTOM&from=...&to=...
```

默认按日返回，缺失日期补 0。全部无记录时 `empty=true`。兼容 `bucket=HOUR&from&to` 小时分桶。

## 使用记录分页

`GET /api/v1/token-usage/me/records?page=1&size=20&provider=GPT&model=gpt-5`

- 页码从 1 开始。
- 默认每页 20 条，最大 100 条。
- 可选 `from`、`to`、`provider`、`model`。
- 返回 `items`、`page`、`size`、`totalElements`、`totalPages`、`empty`。

## 调用详情

`GET /api/v1/token-usage/me/records/{requestId}`

返回用户输入长度、AI 输出长度、各类 Token、模型、厂商、响应时间、调用状态和发生时间。

## 使用率状态

`GET /api/v1/token-usage/me/status`

```text
usageRate = todayTotalTokens / dailyTokenLimit
remainingTokens = max(0, dailyTokenLimit - todayTotalTokens)
```

- `NORMAL`：低于阈值。
- `HIGH`：达到阈值，默认 80%。
- `EXHAUSTED`：达到或超过每日额度。
- `UNCONFIGURED`：额度未配置，此时使用率和剩余额度为 `null`。

失败率达到 `TOKEN_MONITOR_FAILURE_RATE_THRESHOLD` 时，返回 `abnormal=true`、`abnormalReason=HIGH_FAILURE_RATE`。

## SSE

`GET /api/v1/token-usage/me/stream`

连接后立即发送今日汇总；新事件写入后发送最新汇总；15 秒无数据发送心跳。页面重连后应先调用 `/today` 校准。

## 错误

```json
{
  "error": {
    "code": "INVALID_RANGE",
    "message": "from must be before to"
  }
}
```

- `400`：非法字段、分页、时间范围或 Token 总数。
- `401`：缺少用户身份或内部凭证无效。
- `404`：接口或调用详情不存在。
- `502`：厂商额度上游失败；不返回密钥或上游正文。
- `500`：未预期内部错误。

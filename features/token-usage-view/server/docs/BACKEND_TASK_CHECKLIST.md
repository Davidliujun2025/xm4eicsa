# 后端任务验收清单

| 图片要求 | 状态 | 代码/API 证据 |
|---|---|---|
| 今日 Token 消耗统计 | 完成 | `GET /api/v1/token-usage/me/today` |
| 今日/本周/本月/历史汇总 | 完成 | `GET /api/v1/token-usage/me/summary`、`TokenUsageService.overview` |
| 最近 7 天趋势 | 完成 | `range=LAST_7_DAYS`，固定返回 7 个日数据点 |
| 最近 30 天趋势 | 完成 | `range=LAST_30_DAYS`，固定返回 30 个日数据点 |
| 自定义趋势 | 完成 | `range=CUSTOM&from&to`，最大 366 天 |
| 趋势缺失日期补零 | 完成 | `TokenUsageService.dailyTrend` |
| 使用记录查询 | 完成 | `GET /api/v1/token-usage/me/records` |
| 默认每页 20 条 | 完成 | `page=1&size=20`，返回总条数和总页数 |
| 调用详情 | 完成 | `GET /api/v1/token-usage/me/records/{requestId}` |
| 用户输入/AI 输出长度 | 完成 | `inputContentLength`、`outputContentLength` 全链路字段 |
| 模型、Input/Output/Total、响应时间、状态 | 完成 | `TokenUsageEvent`、详情 JSON、MySQL 表 |
| Token 使用率 | 完成 | `usedTokens / dailyTokenLimit`，保留 4 位小数 |
| 达到 80% 高使用率 | 完成 | `HIGH` 状态、`highUsage=true`；阈值可配置 |
| 每客服独立额度 | 完成 | `JdbcTokenQuotaResolver` 读取 MySQL `customer_token_quota`；无记录回退环境变量 |
| 无数据提示和隐藏依据 | 完成 | 汇总、趋势、记录返回 `empty=true` |
| 刷新后读取最新记录 | 完成 | 查询无缓存；假数据测试验证新增事件后汇总立即变化 |
| 数据加载失败异常 | 完成 | 统一 400/401/404/500/502 JSON 错误 |
| 用户数据隔离 | 完成 | 所有查询携带 `userId`，他人详情统一 404 |
| 幂等上报 | 完成 | `idempotencyKey` 主键、内存原子写入、MySQL `ON DUPLICATE KEY UPDATE` |

## 验证证据

- Java 21：`-Xlint:all -Werror`。
- 70 项 Java 断言：汇总、趋势、补零、分页、详情、输入/输出长度、空状态、使用率、每用户额度、异常、隔离、刷新一致性。
- 假数据测试不需要 MySQL 或真实厂商密钥。

# 个人 Token 消耗查看后端

面向 AI 客服工作台的 Java 21 后端。负责接收工作台上报的真实模型 `usage`、按客服隔离存储、统计并提供查询接口。本模块不估算 Token，也不调用模型生成原始消耗数据。

## 已实现功能

- 今日 Input、Output、Cached Input、Total Token 统计。
- 今日、本周、本月、历史累计四张汇总卡片。
- 最近 7 天、最近 30 天、自定义时间范围趋势；按天自动补零。
- 使用记录分页查询，默认每页 20 条；支持时间、厂商、模型筛选。
- 单次调用详情：输入/输出文本长度、Input/Output/Cached/Total Token、模型、厂商、响应时间、状态。
- 使用率、剩余额度、`NORMAL/HIGH/EXHAUSTED/UNCONFIGURED` 状态；默认 80% 为高使用率。
- 空数据标记：汇总、趋势、记录接口均返回 `empty`，前端可隐藏图表和表格。
- 幂等上报、客服数据隔离、统一异常响应、SSE 实时汇总。
- PostgreSQL 建表 SQL；开发和测试可使用线程安全内存仓库。
- Kimi、DeepSeek、Claude、GPT 等厂商 Token 上报模型；厂商余额能力单独列出，不把余额误认为本地消耗。

## 数据边界

1. AI 工作台完成一次模型调用后，从厂商响应读取 Token 使用量。
2. 工作台调用 `POST /api/v1/internal/token-usage/events` 上报。
3. 相同 `idempotencyKey` 可安全重试，不重复计数。
4. 额度管理模块通过 `TokenQuotaResolver` 提供每个客服的每日额度；当前启动配置提供全局额度作为默认实现。
5. 查询接口每次读取最新仓库数据；刷新页面后能看到最新上报记录。

## API

| 方法 | 路径 | 功能 |
|---|---|---|
| `POST` | `/api/v1/internal/token-usage/events` | 工作台上报一次模型调用 |
| `GET` | `/api/v1/token-usage/me/today` | 今日统计、额度和使用率 |
| `GET` | `/api/v1/token-usage/me/summary` | 今日/本周/本月/历史汇总 |
| `GET` | `/api/v1/token-usage/me/summary?from&to` | 自定义时间区间汇总 |
| `GET` | `/api/v1/token-usage/me/trend?range=LAST_7_DAYS` | 最近 7 天趋势 |
| `GET` | `/api/v1/token-usage/me/trend?range=LAST_30_DAYS` | 最近 30 天趋势 |
| `GET` | `/api/v1/token-usage/me/trend?range=CUSTOM&from&to` | 自定义趋势 |
| `GET` | `/api/v1/token-usage/me/records?page=1&size=20` | 分页记录，页码从 1 开始 |
| `GET` | `/api/v1/token-usage/me/records/{requestId}` | 单次调用详情 |
| `GET` | `/api/v1/token-usage/me/status` | 使用率、高使用率及失败异常状态 |
| `GET` | `/api/v1/token-usage/me/stream` | SSE 实时今日汇总 |
| `GET` | `/health` | 健康检查 |

客服接口示例使用 `X-User-Id`，必须由可信网关注入。生产环境应替换为已验证的 JWT/Session Principal。内部上报使用 `X-Internal-Api-Key`。

完整请求字段和错误码见 [docs/API.md](docs/API.md)，机器契约见 [docs/openapi.yaml](docs/openapi.yaml)，验证结果见 [docs/TEST_REPORT.md](docs/TEST_REPORT.md)。

## 构建与测试

要求：JDK 21、支持 C++20 的 `g++`。

```powershell
./scripts/build.ps1
./scripts/test.ps1
```

测试使用内存假数据，不需要数据库或真实厂商密钥。当前覆盖 70 项断言，包括：

- 汇总、趋势、分页、详情、空状态。
- 使用率和 80% 高使用率判断。
- 每客服独立额度解析。
- 幂等、用户隔离、无认证、非法范围。
- 写入后再次查询得到最新数据。
- Java 和 C++ 上报字段契约。

## 运行

内存模式：

```powershell
$env:TOKEN_MONITOR_INTERNAL_API_KEY='replace-me'
$env:TOKEN_MONITOR_DAILY_TOKEN_LIMIT='50000'
./scripts/build.ps1
java --add-modules jdk.httpserver -cp build/classes tokenmonitor.TokenMonitorApplication
```

PostgreSQL 模式：

1. 执行 `backend/db/V1__create_token_usage_event.sql`。
2. 将 PostgreSQL JDBC Driver 放入运行时 classpath。
3. 配置：

```text
TOKEN_MONITOR_JDBC_URL=jdbc:postgresql://127.0.0.1:5432/token_monitor
TOKEN_MONITOR_JDBC_USER=token_monitor
TOKEN_MONITOR_JDBC_PASSWORD=replace-me
```

## 配置

| 环境变量 | 默认值 | 说明 |
|---|---:|---|
| `TOKEN_MONITOR_HOST` | `127.0.0.1` | 监听地址 |
| `TOKEN_MONITOR_PORT` | `8080` | 监听端口 |
| `TOKEN_MONITOR_ZONE` | `Asia/Shanghai` | 业务时区 |
| `TOKEN_MONITOR_INTERNAL_API_KEY` | 无 | 必填；内部上报凭证 |
| `TOKEN_MONITOR_DAILY_TOKEN_LIMIT` | `0` | 默认每日额度；0 表示未配置 |
| `TOKEN_MONITOR_HIGH_USAGE_THRESHOLD` | `0.80` | 高使用率阈值 |
| `TOKEN_MONITOR_FAILURE_RATE_THRESHOLD` | `0.20` | 失败异常阈值 |

真实密钥只放环境变量，不提交仓库。示例见 `.env.example`。

## 目录

```text
backend/
├─ db/V1__create_token_usage_event.sql
└─ src/
   ├─ main/java/tokenmonitor/   # 单层 Java 包
   └─ test/java/tokenmonitor/   # 假数据测试
cpp-client/
├─ include/token_monitor_client.hpp
├─ src/token_monitor_client.cpp
└─ tests/token_monitor_client_test.cpp
docs/
scripts/
```

## 生产接入待办

- 将示例 `X-User-Id` 替换为项目统一登录身份。
- 将默认额度解析器替换为额度管理模块实现。
- 在部署环境执行 PostgreSQL 集成测试和压测。

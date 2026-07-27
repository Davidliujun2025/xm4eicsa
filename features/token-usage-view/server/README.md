# 个人 Token 消耗查看后端

面向 AI 客服工作台的 Java 21 后端。负责接收工作台上报的真实模型 `usage`、按客服隔离存储、统计并提供查询接口。本模块不估算 Token，也不调用模型生成原始消耗数据。

## 快速启动后端服务

### 1. 获取代码并进入服务目录

首次获取项目：

```powershell
git clone https://github.com/Davidliujun2025/xm4eicsa.git
cd xm4eicsa
git switch develop
git pull --ff-only origin develop
cd features/token-usage-view/server
```

已经克隆项目时，从仓库根目录进入：

```powershell
cd features/token-usage-view/server
```

### 2. 准备运行环境

必须安装：

- JDK 21，并确保 `java -version`、`javac -version` 可用。
- PowerShell。
- MySQL 模式需要 MySQL 8.0.19+ 和 MySQL Connector/J；内存模式不需要。

检查环境：

```powershell
java -version
javac -version
```

### 3. 选择运行模式

#### 内存模式

适合本地开发和接口联调。不需要数据库，但服务重启后数据会清空。

在当前 PowerShell 窗口设置环境变量：

```powershell
$env:TOKEN_MONITOR_HOST="127.0.0.1"
$env:TOKEN_MONITOR_PORT="8080"
$env:TOKEN_MONITOR_ZONE="Asia/Shanghai"
$env:TOKEN_MONITOR_INTERNAL_API_KEY="local-dev-key"
$env:TOKEN_MONITOR_DAILY_TOKEN_LIMIT="50000"
```

`TOKEN_MONITOR_INTERNAL_API_KEY` 必须设置，否则程序会拒绝启动。

#### 使用 MySQL 8 持久化

1. 启动 MySQL 8.0.19+。
2. 在 MySQL Workbench 中执行
   [`backend/db/V1__create_token_usage_event.sql`](backend/db/V1__create_token_usage_event.sql)，
   或在 MySQL 客户端中执行：

```sql
SOURCE backend/db/V1__create_token_usage_event.sql;
```

3. 将 MySQL Connector/J 的 JAR 放到 `lib/`，并命名为
   `mysql-connector-j.jar`。
4. 设置连接信息：

```powershell
$env:TOKEN_MONITOR_HOST="127.0.0.1"
$env:TOKEN_MONITOR_PORT="8080"
$env:TOKEN_MONITOR_ZONE="Asia/Shanghai"
$env:TOKEN_MONITOR_INTERNAL_API_KEY="local-dev-key"
$env:TOKEN_MONITOR_DAILY_TOKEN_LIMIT="50000"

$env:TOKEN_MONITOR_JDBC_URL="jdbc:mysql://REPLACE_WITH_XM4_HOST:3306/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true&requireSSL=true&enabledTLSProtocols=TLSv1.3&verifyServerCertificate=true"
$env:TOKEN_MONITOR_JDBC_USER="token_monitor"
$env:TOKEN_MONITOR_JDBC_PASSWORD="replace-me"
```

数据库账号需要能够读写 `ai_customer_service.token_usage_event`，并读取
`ai_customer_service.customer_token_quota`。

> `.env.example` 只是配置示例。当前 Java 程序直接读取系统环境变量，不会自动加载
> `.env.example` 或 `.env`。

### 4. 构建服务

完整构建：

```powershell
./scripts/build.ps1
```

该脚本会使用 `-Xlint:all -Werror` 严格编译 Java 后端。成功时输出：

```text
Build passed.
```

运行全部自动化测试：

```powershell
./scripts/test.ps1
```

预期输出：

```text
Build passed.
PASS: 70 assertions
All tests passed.
```

### 5. 启动服务

内存模式：

```powershell
java --add-modules jdk.httpserver -cp build/classes tokenmonitor.TokenMonitorApplication
```

MySQL 模式：

```powershell
java --add-modules jdk.httpserver -cp "build/classes;lib/mysql-connector-j.jar" tokenmonitor.TokenMonitorApplication
```

Windows classpath 使用分号 `;`。Linux/macOS 使用冒号 `:`。

启动成功后，终端保持运行并显示：

```text
Personal Token Monitor listening on http://127.0.0.1:8080
```

默认服务地址：

```text
http://127.0.0.1:8080
```

如果修改了 `TOKEN_MONITOR_HOST` 或 `TOKEN_MONITOR_PORT`，后续请求地址也要同步修改。

### 6. 检查服务是否可用

保持服务窗口运行，另开一个 PowerShell：

```powershell
$baseUrl = "http://127.0.0.1:8080"
Invoke-RestMethod "$baseUrl/health"
```

预期返回：

```json
{
  "status": "UP",
  "time": "2026-07-25T00:00:00Z"
}
```

检查端口监听：

```powershell
Get-NetTCPConnection -State Listen -LocalPort 8080
```

### 7. 上报一条本地测试数据

内部上报接口必须携带与启动配置一致的 `X-Internal-Api-Key`：

```powershell
$baseUrl = "http://127.0.0.1:8080"
$requestId = [guid]::NewGuid().ToString()

$body = @{
    idempotencyKey       = $requestId
    userId               = "agent-1001"
    conversationId       = "conversation-20"
    requestId            = $requestId
    provider             = "KIMI"
    model                = "moonshot-v1"
    inputContentLength   = 420
    outputContentLength  = 110
    inputTokens          = 1200
    outputTokens         = 230
    cachedInputTokens    = 800
    totalTokens          = 1430
    responseTimeMs       = 860
    status               = "SUCCEEDED"
    occurredAt           = (Get-Date).ToUniversalTime().ToString("o")
    providerReportedCost = 0.0042
    costCurrency         = "USD"
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/api/v1/internal/token-usage/events" `
    -Headers @{ "X-Internal-Api-Key" = "local-dev-key" } `
    -ContentType "application/json" `
    -Body $body
```

首次上报返回 HTTP `201`；重复使用相同 `idempotencyKey` 时返回 HTTP `200` 和
`duplicate=true`，不会重复统计。

### 8. 查询个人 Token 使用情况

开发环境通过 `X-User-Id` 模拟当前登录客服。该值必须与上报数据中的 `userId`
一致：

```powershell
$headers = @{ "X-User-Id" = "agent-1001" }

Invoke-RestMethod `
    -Uri "$baseUrl/api/v1/token-usage/me/today" `
    -Headers $headers
```

常用查询：

```powershell
# 今日统计
Invoke-RestMethod "$baseUrl/api/v1/token-usage/me/today" -Headers $headers

# 今日、本周、本月、历史汇总
Invoke-RestMethod "$baseUrl/api/v1/token-usage/me/summary" -Headers $headers

# 最近 7 天趋势
Invoke-RestMethod "$baseUrl/api/v1/token-usage/me/trend?range=LAST_7_DAYS" -Headers $headers

# 使用记录，默认每页 20 条
Invoke-RestMethod "$baseUrl/api/v1/token-usage/me/records?page=1&size=20" -Headers $headers

# 使用率和异常状态
Invoke-RestMethod "$baseUrl/api/v1/token-usage/me/status" -Headers $headers
```

完整字段、过滤参数和错误码见 [`docs/API.md`](docs/API.md)，OpenAPI 契约见
[`docs/openapi.yaml`](docs/openapi.yaml)。

### 9. 允许其他机器访问

默认 `TOKEN_MONITOR_HOST=127.0.0.1`，只能从本机访问。局域网或容器环境需要设置：

```powershell
$env:TOKEN_MONITOR_HOST="0.0.0.0"
```

然后使用服务器实际 IP 和端口访问，例如：

```text
http://192.168.1.20:8080/health
```

开放访问前需要配置 Windows 防火墙、安全组或容器端口映射。生产环境应放在可信
网关后面，不应直接暴露到公网；`X-User-Id` 必须替换为经过验证的 JWT 或 Session
身份。

### 10. 停止服务

服务在前台运行时，在启动服务的 PowerShell 窗口按 `Ctrl+C`。

### 常见问题

#### 启动时报 `TOKEN_MONITOR_INTERNAL_API_KEY must be configured`

当前 PowerShell 没有设置内部密钥：

```powershell
$env:TOKEN_MONITOR_INTERNAL_API_KEY="local-dev-key"
```

#### `8080` 端口被占用

查找占用端口的进程：

```powershell
Get-NetTCPConnection -State Listen -LocalPort 8080 |
    Select-Object LocalAddress, LocalPort, OwningProcess
```

可以改用其他端口：

```powershell
$env:TOKEN_MONITOR_PORT="8081"
```

#### 请求返回 `401`

- 客服查询接口缺少 `X-User-Id`。
- 内部上报或厂商额度接口缺少正确的 `X-Internal-Api-Key`。

#### 内存模式重启后数据消失

这是预期行为。配置 `TOKEN_MONITOR_JDBC_URL`、数据库账号和 Connector/J 后使用
MySQL 模式。

#### MySQL 模式启动或访问时报 JDBC 驱动错误

确认：

- `lib/mysql-connector-j.jar` 存在。
- 启动命令的 classpath 包含该 JAR。
- JDBC URL、账号、密码正确。
- MySQL 服务已启动，DDL 已执行。

#### 其他机器无法访问

确认监听地址不是 `127.0.0.1`，并检查防火墙、安全组和端口映射。

## 已实现功能

- 今日 Input、Output、Cached Input、Total Token 统计。
- 今日、本周、本月、历史累计四张汇总卡片。
- 最近 7 天、最近 30 天、自定义时间范围趋势；按天自动补零。
- 使用记录分页查询，默认每页 20 条；支持时间、厂商、模型筛选。
- 单次调用详情：输入/输出文本长度、Input/Output/Cached/Total Token、模型、厂商、响应时间、状态。
- 使用率、剩余额度、`NORMAL/HIGH/EXHAUSTED/UNCONFIGURED` 状态；默认 80% 为高使用率。
- 空数据标记：汇总、趋势、记录接口均返回 `empty`，前端可隐藏图表和表格。
- 幂等上报、客服数据隔离、统一异常响应、SSE 实时汇总。
- MySQL 8 建表 SQL；开发和测试可使用线程安全内存仓库。
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

要求：JDK 21。

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
- Java 上报字段及 HTTP 接口契约。

## 运行

内存模式：

```powershell
$env:TOKEN_MONITOR_INTERNAL_API_KEY='replace-me'
$env:TOKEN_MONITOR_DAILY_TOKEN_LIMIT='50000'
./scripts/build.ps1
java --add-modules jdk.httpserver -cp build/classes tokenmonitor.TokenMonitorApplication
```

MySQL 8 模式：

1. 执行 `backend/db/V1__create_token_usage_event.sql`。
2. 将 MySQL Connector/J 放入运行时 classpath。
3. 配置：

```text
TOKEN_MONITOR_JDBC_URL=jdbc:mysql://REPLACE_WITH_XM4_HOST:3306/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true&requireSSL=true&enabledTLSProtocols=TLSv1.3&verifyServerCertificate=true
TOKEN_MONITOR_JDBC_USER=token_monitor
TOKEN_MONITOR_JDBC_PASSWORD=replace-me
```

MySQL 模式下，个人额度优先读取 `customer_token_quota`；无有效记录时回退
`TOKEN_MONITOR_DAILY_TOKEN_LIMIT`。`occurred_at` 按 UTC 写入，统计时再按
`TOKEN_MONITOR_ZONE` 切分自然日。

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
docs/
scripts/
```

## 生产接入待办

- 将示例 `X-User-Id` 替换为项目统一登录身份。
- 由额度管理模块维护 `customer_token_quota`。
- 在部署环境执行 MySQL 8 集成测试和压测。

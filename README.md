# CarePilot — AI 智能客服系统

基于 **"客服五步法"** 的 AI 智能客服助手，帮助电商客服人员快速生成专业的客服话术。

## 核心功能

| 步骤 | 名称 | 说明 |
|------|------|------|
| 第 1 步 | 意图识别 | 分析客户问题的核心诉求 |
| 第 2 步 | 回复策略 | 制定整体回复方向和策略 |
| 第 3 步 | 推荐话术 | 生成可直接使用的客服回复 |
| 第 4 步 | 钩子引导 | 设计下一步互动钩子 |
| 第 5 步 | 成功收尾 | 生成满意度收尾话术 |

### 对话管理

- 按平台（淘宝、京东、拼多多、抖音、天猫等）创建客服对话
- 支持五步串行生成：每步基于前序结果动态生成
- 每个步骤可独立编辑（body、label、tags）、自动静默保存
- 对话历史列表支持关键词搜索和分页

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 3.3.5 |
| Java | 21 |
| ORM | MyBatis-Plus 3.5.9 |
| 数据库 | MySQL 8.0 |
| AI（框架阶段） | Stub（占位），后续接入通义千问 DashScope |
| 构建 | Maven 3.9+ |
| 测试 | JUnit 5 + MockMvc |

## 项目结构

```
carepilot-server/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/carepilot/
    │   │   ├── CarePilotApplication.java          # 启动入口
    │   │   ├── common/
    │   │   │   ├── Result.java                    # 统一响应体
    │   │   │   ├── PageResult.java                # 分页响应体
    │   │   │   └── exception/
    │   │   │       ├── BusinessException.java     # 业务异常
    │   │   │       └── GlobalExceptionHandler.java # 全局异常处理
    │   │   ├── config/
    │   │   │   ├── MyBatisPlusConfig.java         # MyBatis-Plus 配置
    │   │   │   └── WebConfig.java                 # CORS 跨域配置
    │   │   ├── infra/
    │   │   │   ├── ai/
    │   │   │   │   ├── QwenClient.java            # AI 调用接口
    │   │   │   │   ├── StubQwenClient.java        # AI 占位实现
    │   │   │   │   ├── PromptBuilder.java         # Prompt 组装器
    │   │   │   │   └── AiGenerationException.java # AI 异常
    │   │   │   └── auth/
    │   │   │       └── AuthContext.java           # 用户身份（Mock）
    │   │   └── module/conversation/
    │   │       ├── controller/
    │   │       │   └── ConversationController.java
    │   │       ├── service/
    │   │       │   ├── ConversationService.java
    │   │       │   └── impl/ConversationServiceImpl.java
    │   │       ├── mapper/
    │   │       │   ├── ConversationMapper.java
    │   │       │   └── ConversationStepMapper.java
    │   │       ├── entity/
    │   │       │   ├── Conversation.java
    │   │       │   └── ConversationStep.java
    │   │       └── dto/
    │   │           ├── CreateConversationRequest.java
    │   │           ├── SaveStepsRequest.java
    │   │           ├── ConversationListItem.java
    │   │           └── ConversationDetailResponse.java
    │   └── resources/
    │       └── application.yml
    └── test/java/com/carepilot/
        └── ConversationApiTests.java              # 接口测试用例
```

## API 文档

Base URL: `http://localhost:8080`

### 3.1 查询对话历史列表

```http
GET /api/v1/conversations?page=1&size=20&keyword=退货&category=售后
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 20 |
| keyword | string | 否 | 搜索关键词（匹配客户问题） |
| category | string | 否 | 分类筛选，"全部" 除外 |

### 3.2 查询单条对话详情

```http
GET /api/v1/conversations/{id}
```

### 3.3 创建对话并生成第 1 步

```http
POST /api/v1/conversations
Content-Type: application/json

{
  "platform": "淘宝",
  "question": "客户收到货后发现商品有划痕，要求退货退款"
}
```

### 3.4 生成下一步骤

```http
POST /api/v1/conversations/{id}/generate-next
```

依次调用可生成第 2~5 步。第 5 步完成后对话状态变为 `success`，无法继续生成。

### 3.5 静默保存编辑内容

```http
PUT /api/v1/conversations/{id}
Content-Type: application/json

{
  "steps": [
    {
      "stepId": 1,
      "stepOrder": 1,
      "body": "客服应首先安抚客户情绪...",
      "label": "已优化",
      "tags": ["售后", "退货", "安抚"]
    }
  ]
}
```

### 统一响应格式

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 业务异常
{ "code": 40401, "message": "对话不存在", "data": null }

// 参数校验失败
{ "code": 40001, "message": "参数校验失败：客户问题不能为空", "data": null }

// 服务器错误
{ "code": 50000, "message": "服务器内部错误", "data": null }
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 8.0+（需创建 `carepilot` 数据库并建表）

### 数据库

```sql
CREATE DATABASE IF NOT EXISTS carepilot DEFAULT CHARSET utf8mb4;

USE carepilot;

CREATE TABLE conversation (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    platform    VARCHAR(32)  NOT NULL,
    question    VARCHAR(500) NOT NULL,
    category    VARCHAR(32)  NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'generating',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_category (category)
);

CREATE TABLE conversation_step (
    id               BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id  BIGINT      NOT NULL,
    user_id          BIGINT      NOT NULL,
    step_order       TINYINT     NOT NULL,
    step_type        VARCHAR(16) NOT NULL,
    title            VARCHAR(64) NULL,
    label            VARCHAR(32) NULL,
    tags             JSON        NULL,
    section          VARCHAR(32) NULL,
    body             TEXT        NOT NULL,
    risk_warning     JSON        NULL,
    status           VARCHAR(64) NULL,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id)
);

CREATE TABLE favorite (
    id               BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    conversation_id  BIGINT      NULL,
    step_id          BIGINT      NOT NULL,
    step_type        VARCHAR(16) NOT NULL,
    content          TEXT        NOT NULL,
    platform         VARCHAR(32) NOT NULL,
    category         VARCHAR(32) NULL,
    question         VARCHAR(500) NULL,
    collected_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
);
```

### 启动

```bash
# 1. 克隆并进入目录
cd carepilot-server

# 2. 修改数据库连接（如需要）
# 编辑 src/main/resources/application.yml
# spring.datasource.url / username / password

# 3. 启动
mvn spring-boot:run

# 4. 访问
# http://localhost:8080/api/v1/conversations
```

### 运行测试

```bash
mvn test
```

## 分工

| 模块 | 负责人 | 当前状态 |
|------|--------|----------|
| 后端 carepilot-server | 当前 | 对话五步法框架已完成，AI 为占位实现 |
| AI 接入层（DashScope） | AI 同学 | 待替换 StubQwenClient |
| 前端 | 前端同学 | 待开发 |

## 待办

- [ ] 接入通义千问 DashScope API，替换 StubQwenClient
- [ ] Spring Security + JWT 鉴权，替换 AuthContext 的 Mock
- [ ] Redis 缓存 + Token 用量统计
- [ ] 对话导出（PDF/Word）
- [ ] 话术收藏夹完善
- [ ] 单元测试覆盖 Service 层

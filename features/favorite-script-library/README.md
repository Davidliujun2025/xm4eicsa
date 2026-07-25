# AI智能客服系统 - 话术库收藏后端原型

这是一个 Java 21 + Spring Boot 3 的后端原型，用于支持 AI 工作台生成回复话术后的收藏、取消收藏、个人话术库查看和标签检索。

## 技术栈

- Java 21
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- H2 内存数据库
- Bean Validation
- Springdoc OpenAPI

## 启动

端口已固定为 `8088`。

推荐先打包再启动，这种方式在包含中文的 Windows 路径下更稳定：

```bash
mvn test
mvn package -DskipTests
java -jar target/ai-customer-service-0.0.1-SNAPSHOT.jar
```

如果项目路径没有中文，也可以直接使用：

```bash
mvn spring-boot:run
```

启动后可访问：

- API Base URL: `http://localhost:8088/api/v1`
- Swagger UI: `http://localhost:8088/swagger-ui/index.html`
- H2 Console: `http://localhost:8088/h2-console`

H2 连接信息：

- JDBC URL: `jdbc:h2:mem:script_library`
- User Name: `sa`
- Password: 空

## 前端对接约定

个人话术库通过请求头区分客服：

```http
X-Staff-Id: csr-001
```

如果不传，后端会使用默认客服 `demo-csr`，方便本地联调。

## 1. 收藏或取消收藏

```http
POST /api/v1/script-favorites/toggle
Content-Type: application/json
X-Staff-Id: csr-001
```

请求体：

```json
{
  "sourceTalkId": "ai-talk-10001",
  "content": "亲，您先别着急。关于退货问题，我们会优先帮您核实订单状态，并尽快给您处理方案。",
  "scenario": "退货安抚",
  "generatedAt": "2026-07-22T10:20:30Z",
  "tags": ["退货安抚", "售后"]
}
```

说明：

- `sourceTalkId` 是 AI 工作台生成的话术 ID，建议前端传入。
- 如果 AI 工作台暂时没有话术 ID，后端会用 `content` 的 hash 识别同一条话术。
- 首次收藏时 `tags` 至少传 1 个。
- 每个标签最多 5 个字符，如 `售前咨询`、`物流问题`。
- 再次点击同一条话术会取消收藏，取消时可以不传 `tags`。

收藏成功响应：

```json
{
  "favorited": true,
  "favorite": {
    "id": "7df51c4d-88f9-465a-b186-cd9492b55eba",
    "sourceTalkId": "ai-talk-10001",
    "content": "亲，您先别着急。关于退货问题，我们会优先帮您核实订单状态，并尽快给您处理方案。",
    "scenario": "退货安抚",
    "generatedAt": "2026-07-22T10:20:30Z",
    "tags": ["退货安抚", "售后"],
    "createdAt": "2026-07-22T10:21:00Z",
    "lastUsedAt": "2026-07-22T10:21:00Z"
  },
  "message": "已收藏"
}
```

取消收藏响应：

```json
{
  "favorited": false,
  "favorite": null,
  "message": "已取消收藏"
}
```

## 2. 查询个人话术库

```http
GET /api/v1/script-favorites?page=0&size=20
X-Staff-Id: csr-001
```

按关键词检索，关键词会匹配话术内容或标签：

```http
GET /api/v1/script-favorites?keyword=退货安抚
X-Staff-Id: csr-001
```

按标签检索：

```http
GET /api/v1/script-favorites?tag=售后
X-Staff-Id: csr-001
```

响应按 `lastUsedAt` 最近使用时间倒序排列。

## 3. 标记最近使用时间

客服复用某条收藏话术时，前端可以调用该接口刷新排序依据。

```http
POST /api/v1/script-favorites/{favoriteId}/use
X-Staff-Id: csr-001
```

## 4. 查询全部标签

```http
GET /api/v1/script-favorites/tags
X-Staff-Id: csr-001
```

## 5. 删除收藏

```http
DELETE /api/v1/script-favorites/{favoriteId}
X-Staff-Id: csr-001
```

## 容量上限

默认每个客服最多收藏 `200` 条，配置位置：

```yaml
app:
  script-library:
    max-size: 200
```

达到上限后，新收藏会返回 `409 Conflict`：

```json
{
  "message": "话术库已达上限，请清理旧话术",
  "errors": {},
  "timestamp": "2026-07-22T10:30:00Z"
}
```

## 验收标准覆盖

- AI 工作台生成话术后，前端调用 toggle 接口，可在后端保存完整文本、适用场景、生成时间和标签。
- 再次点击同一条话术可取消收藏。
- 新增收藏时至少需要 1 个标签，标签最多 5 个字符。
- `keyword` 仅匹配收藏话术的内容或标签。
- 列表按最近使用时间倒序。
- 超过容量上限时返回固定提示：`话术库已达上限，请清理旧话术`。

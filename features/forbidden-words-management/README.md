# forbidden-words-management

违禁词库管理功能实现（前端 JavaScript + 后端 Java）。

## 目录

- `ui/`：管理后台页面（纯 JavaScript）
- `server/`：Spring Boot 服务（Java 21）

## 运行后端

```bash
cd features/forbidden-words-management/server
mvn spring-boot:run
```

如果本机没有全局 `mvn`，可使用本地 Maven：

```bash
cd features/forbidden-words-management/server
$HOME/.local/tools/apache-maven-3.9.9/bin/mvn spring-boot:run
```

默认地址：`http://localhost:8081`

### MySQL（xm4）配置

后端默认使用 `mysql` profile，并连接 xm4 数据库。

推荐启动命令：

```bash
cd features/forbidden-words-management/server
XM4_MYSQL_URL='jdbc:mysql://127.0.0.1:3307/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
XM4_MYSQL_USERNAME='root' \
XM4_MYSQL_PASSWORD='123456' \
mvn spring-boot:run
```

如果没有全局 `mvn`，可改为：

```bash
cd features/forbidden-words-management/server
XM4_MYSQL_URL='jdbc:mysql://127.0.0.1:3307/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
XM4_MYSQL_USERNAME='root' \
XM4_MYSQL_PASSWORD='123456' \
$HOME/.local/tools/apache-maven-3.9.9/bin/mvn spring-boot:run
```

说明：服务启动时会自动执行 `schema-mysql.sql`，在 xm4 中创建 `forbidden_word`、`operation_audit_log`、`hit_audit_log` 三张表（若不存在）。

## 运行前端

可直接使用任意静态服务打开 `ui/` 目录，例如：

```bash
cd features/forbidden-words-management/ui
python3 -m http.server 5500
```

访问：`http://localhost:5500`

前端默认请求：`http://127.0.0.1:8081/api`

## 关键接口

- `GET /api/forbidden-words?platform=ALL&page=1&size=20`：平台筛选 + 分页
- `POST /api/forbidden-words`：新增违禁词（记录操作人、时间、IP）
- `DELETE /api/forbidden-words/{id}`：物理删除并触发缓存刷新
- `POST /api/forbidden-words/csv/preview`：CSV 预览与错误高亮
- `POST /api/forbidden-words/csv/confirm`：确认后批量导入
- `GET /api/forbidden-words/audit/operations`：操作审计日志查询
- `POST /api/chat-audit/check`：聊天输入/输出违禁词命中审计
- `GET /api/chat-audit/logs`：违禁词触发审计日志查询

## 验收标准映射

1. 平台筛选 + 每页20条分页：`GET /api/forbidden-words`
2. 新增2秒内入库 + 操作日志：前端新增耗时检查 + 后端操作日志记录
3. 移除二次确认 + 物理删除 + 5分钟缓存窗口：前端 `confirm`，后端 `DELETE` + `cacheSyncWindowMinutes=5`
4. CSV 20条预览 + 错误高亮 + 确认导入：`/csv/preview` + `/csv/confirm`
5. 可追溯删除记录：`/audit/operations?action=DELETE`
6. 聊天问答触发检测 + 处罚追溯 + 多次提醒：`/chat-audit/check` + `/chat-audit/logs`
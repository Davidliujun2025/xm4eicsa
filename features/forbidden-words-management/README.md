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

默认地址：`http://localhost:8080`

### MySQL 预留说明（当前可不启用）

后端已预留 MySQL 连接配置（`mysql` profile），并加入驱动依赖。

- 默认运行：`local` profile，不依赖数据库
- 启用 MySQL：

```bash
cd features/forbidden-words-management/server
MYSQL_URL='jdbc:mysql://localhost:3306/forbidden_words?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
MYSQL_USERNAME='root' \
MYSQL_PASSWORD='your_password' \
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

如果没有全局 `mvn`，可改为：

```bash
cd features/forbidden-words-management/server
MYSQL_URL='jdbc:mysql://localhost:3306/forbidden_words?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
MYSQL_USERNAME='root' \
MYSQL_PASSWORD='your_password' \
$HOME/.local/tools/apache-maven-3.9.9/bin/mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

说明：当前业务数据仍以内存实现为主，MySQL 配置先行预留，待你后续建库后可平滑切换到持久化实现。

## 运行前端

可直接使用任意静态服务打开 `ui/` 目录，例如：

```bash
cd features/forbidden-words-management/ui
python3 -m http.server 5500
```

访问：`http://localhost:5500`

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
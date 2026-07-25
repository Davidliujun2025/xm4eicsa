# 收藏话术库数据库设计

功能目录：`backend`

是否需要数据库：是

数据库：MySQL 8.0.19+。使用行别名 Upsert；`CHECK` 约束要求 MySQL 8.0.16+。

数据库名：`ai_customer_service`

API 前缀：

- 收藏或取消收藏：`/api/v1/script-favorites/toggle`
- 个人话术库查询：`/api/v1/script-favorites`
- 个人标签查询：`/api/v1/script-favorites/tags`
- 标记最近使用：`/api/v1/script-favorites/{favoriteId}/use`

预计需要 3 张表。

缓存：不强依赖 Redis。个人话术库容量较小，收藏、取消、标签查询和简单检索直接读写 MySQL。

## 表 1：script_library_config

用途：保存个人话术库容量上限。默认每个客服最多收藏 200 条话术；达到上限时，应用层返回提示：`话术库已达上限，请清理旧话术`。

字段：

- `id`：固定为 `1`，单例配置。
- `max_favorite_count`：每个客服最多收藏数量。
- `updated_by`：最后修改人 ID。
- `created_at`：创建时间，UTC。
- `updated_at`：更新时间，UTC。

约束与索引：

- `id = 1`，保证只有一条配置。
- `max_favorite_count > 0`。

主要操作：

- 系统初始化时写入默认上限。
- 后台配置调整时使用 MySQL 8.0.19+ 行别名 Upsert。
- 新收藏前读取该配置并比较当前收藏数量。

## 表 2：script_favorite

用途：保存客服收藏的 AI 回复话术，包括完整文本、适用场景、生成时间、收藏时间和最近使用时间。

字段：

- `id`：自增主键。
- `user_id`：客服用户 ID，所有个人查询必须携带此条件，保证数据隔离。
- `source_talk_id`：AI 工作台生成的话术 ID，可为空；前端能提供时优先使用。
- `content_hash`：话术完整文本的 SHA-256 哈希；用于 `source_talk_id` 暂缺时识别重复话术。
- `content`：话术完整文本。
- `scenario`：适用场景，如 `退货安抚`。
- `generated_at`：AI 生成话术时间，UTC。
- `created_at`：收藏时间，UTC。
- `updated_at`：更新时间，UTC。
- `last_used_at`：最近使用时间，用于话术库排序。

约束与索引：

- `(user_id, source_talk_id)` 唯一，避免同一客服重复收藏同一条 AI 话术。
- `(user_id, content_hash)` 唯一，用于无 `source_talk_id` 时兜底去重。
- `(user_id, last_used_at, id)` 支持个人库按最近使用时间倒序分页。
- `content_hash` 必须为 64 位小写十六进制 SHA-256。
- `content` 和 `scenario` 不能为空白文本。

主要操作：

- 点击收藏时，先按 `user_id + source_talk_id/content_hash` 判断是否已收藏。
- 未收藏时，检查个人库数量是否小于上限，再插入主表和标签表。
- 已收藏时，删除主表记录；标签表通过外键级联删除。
- 话术被复用时，刷新 `last_used_at`。

## 表 3：script_favorite_tag

用途：保存收藏话术的标签。一条收藏话术至少有一个标签；最小数量由应用层校验。

字段：

- `favorite_id`：收藏话术 ID。
- `tag`：标签名称，最多 5 个字符。
- `created_at`：创建时间，UTC。

约束与索引：

- `(favorite_id, tag)` 作为主键，避免同一条话术重复绑定同一标签。
- `tag` 长度为 1 到 5 个字符。
- 外键引用 `script_favorite(id)`，主表删除时级联删除标签。
- `(tag, favorite_id)` 支持按标签检索。

主要操作：

- 收藏弹窗提交后批量插入标签。
- 标签选择框通过当前客服已收藏标签去重查询得到。
- 关键词检索时，匹配 `script_favorite.content` 或 `script_favorite_tag.tag`。

## SQL 文件

完整 DDL 和主要操作 SQL 见：

`database/script_favorite_library.sql`

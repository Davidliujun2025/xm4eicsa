# 模型厂商额度 API 核对

核对日期：2026-07-18。这里只记录官方公开接口；网页控制台能力不等于可编程 API。

| 厂商 | 账户余额 | Token/订阅套餐剩余 | 历史用量 | 实现结论 |
|---|---|---|---|---|
| Kimi / Moonshot | 支持 | 未发现公开接口 | 未发现公开接口 | 已接 `GET https://api.moonshot.cn/v1/users/me/balance` |
| Claude / Anthropic | 未发现公开接口 | 未发现 Claude Plan 剩余接口 | Admin Usage/Cost API | 不伪造余额；个人账户也不能依赖 Admin API |
| GPT / OpenAI | 未发现公开接口 | 未发现 ChatGPT/Scale plan 剩余接口 | Organization Usage/Costs API | Token 监控用工作台逐请求 usage；组织费用可另加 Admin Key 适配器 |
| DeepSeek | 支持 | 未发现公开接口 | 未发现公开接口 | 已接 `GET https://api.deepseek.com/user/balance` |
| MiniMax | 未确认 PAYG 余额接口 | 支持 Token Plan | 未发现通用历史用量接口 | 已接 `GET https://www.minimax.io/v1/token_plan/remains` |
| Grok / xAI | 支持团队预付余额 | 未发现订阅 Token Plan 接口 | Management API 支持团队历史用量 | 已接团队预付余额；需要独立 Management Key 与 Team ID |
| GLM / 智谱 | 未发现公开接口 | 未发现公开资源包剩余接口 | 未发现公开接口 | 只保留能力状态，不调用非官方/逆向接口 |

官方依据：

- [Kimi 查询余额](https://platform.kimi.com/docs/api/balance)
- [DeepSeek Get User Balance](https://api-docs.deepseek.com/api/get-user-balance/)
- [MiniMax Token Plan](https://platform.minimax.io/subscribe/token-plan)
- [OpenAI Usage and Costs](https://platform.openai.com/docs/api-reference/usage)
- [Anthropic Admin API](https://platform.claude.com/docs/en/manage-claude/admin-api)
- [xAI Management API Billing](https://docs.x.ai/developers/rest-api-reference/management/billing)
- [智谱开放文档](https://docs.bigmodel.cn/)

## 关键区别

1. 模型调用响应中的 `usage`：单次 Input/Output/Total Token，最适合个人实时监控。
2. 厂商 Usage/Cost Admin API：组织级、可能延迟、通常要求管理员密钥，不能代替客服个人实时记录。
3. 账户现金余额：货币余额，不等于 Token 数。
4. Token Plan：可能按请求、5 小时窗口、周窗口或不同模态计量，不能强制换算成 Token。

因此统一返回 `quotaType` 与 `details`，不把不同额度强制压成同一个 `remainingTokens` 字段。

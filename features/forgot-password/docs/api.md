# 找回密码接口说明

## 1. 基本约定

功能目录：`features/forgot-password`

推荐后端接口前缀：

```text
/api/password
```

说明：仓库文档中为找回密码预留的稳定前缀是 `/api/password/**`，本模块前后端已经统一使用该前缀。

统一响应格式建议跟登录模块保持一致：

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {}
}
```

失败响应：

```json
{
  "success": false,
  "code": "USER_NOT_FOUND",
  "message": "用户不存在",
  "data": null
}
```

## 2. 校验账号

接口：

```text
POST /api/password/account/verify
```

用途：用户从登录页点击“忘记密码”前或进入重置页后，先校验账号格式是否正确、账号是否存在、账号是否处于可用状态。校验通过后生成本次找回密码临时凭证 `resetToken`。

请求体：

```json
{
  "username": "13800138000"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 客服登录账号。当前登录模块中账号为手机号，格式为 11 位中国大陆手机号。 |

成功响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "resetToken": "random-reset-token",
    "username": "13800138000",
    "maskedBoundPhone": "138****8000",
    "expiresInSeconds": 1800
  }
}
```

错误码：

| code | message | 触发场景 |
| --- | --- | --- |
| `USERNAME_INVALID` | 用户名格式不正确 | 用户名为空或不是 11 位手机号格式。 |
| `USER_NOT_FOUND` | 用户不存在 | 数据库中找不到该客服账号。 |
| `ACCOUNT_DISABLED` | 账号已禁用，请联系管理员 | 账号存在但状态不是 `ACTIVE`。 |

## 3. 获取短信验证码

接口：

```text
POST /api/password/sms-code
```

用途：用户输入绑定手机号后，后端校验手机号是否属于当前账号，校验通过后发送 6 位短信验证码。按钮进入 60 秒倒计时，倒计时期间不可重复发送。

请求体：

```json
{
  "resetToken": "random-reset-token",
  "phone": "13800138000"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resetToken` | string | 是 | 账号校验成功后返回的临时凭证。 |
| `phone` | string | 是 | 用户输入的绑定手机号。 |

成功响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "验证码已发送",
  "data": {
    "expiresInSeconds": 300,
    "cooldownSeconds": 60
  }
}
```

错误码：

| code | message | 触发场景 |
| --- | --- | --- |
| `RESET_TOKEN_INVALID` | 重置凭证已失效，请重新验证账号 | `resetToken` 为空、错误、过期或已使用。 |
| `PHONE_REQUIRED` | 请输入绑定手机号 | 用户未输入手机号时点击“获取验证码”。 |
| `PHONE_NOT_BOUND` | 该手机号未绑定任何客服账号 | 手机号在账号表中不存在。 |
| `PHONE_NOT_MATCH` | 该手机号未绑定当前账号 | 手机号存在，但不是当前账号绑定手机号。 |
| `SMS_CODE_COOLDOWN` | 验证码已发送，请稍后再试 | 60 秒内重复点击获取验证码。 |

## 4. 提交密码重置

接口：

```text
POST /api/password/confirm
```

用途：用户填写手机号、短信验证码、新密码和确认新密码后，后端验证验证码和密码规则。全部通过后更新客服账号密码哈希，并让本次 `resetToken` 失效。

请求体：

```json
{
  "resetToken": "random-reset-token",
  "phone": "13800138000",
  "smsCode": "123456",
  "newPassword": "Aa123456789!",
  "confirmPassword": "Aa123456789!"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resetToken` | string | 是 | 本次找回密码临时凭证。 |
| `phone` | string | 是 | 账号绑定手机号。 |
| `smsCode` | string | 是 | 6 位短信验证码。 |
| `newPassword` | string | 是 | 新密码。 |
| `confirmPassword` | string | 是 | 再次输入的新密码。 |

成功响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "密码重置成功",
  "data": {
    "reset": true,
    "nextAction": "BACK_TO_LOGIN"
  }
}
```

错误码：

| code | message | 触发场景 |
| --- | --- | --- |
| `RESET_TOKEN_INVALID` | 重置凭证已失效，请重新验证账号 | 凭证错误、过期或已使用。 |
| `PHONE_REQUIRED` | 请输入绑定手机号 | 手机号为空。 |
| `PHONE_NOT_BOUND` | 该手机号未绑定任何客服账号 | 手机号不存在。 |
| `PHONE_NOT_MATCH` | 该手机号未绑定当前账号 | 手机号和账号不匹配。 |
| `SMS_CODE_EMPTY` | 请输入短信验证码 | 验证码为空。 |
| `SMS_CODE_INVALID` | 验证码为空或错误 | 验证码错误、过期或已使用。 |
| `PASSWORD_NOT_MATCH` | 两次输入的新密码不一致 | 新密码和确认密码不同。 |
| `PASSWORD_INVALID` | 密码需为 12-20 位，且至少包含三类字符 | 密码不满足规则。 |

## 5. 密码规则

新密码规则：

- 长度 12-20 位。
- 不允许包含空格。
- 大写字母、小写字母、数字、特殊符号中至少包含三类。
- 新密码和确认新密码必须完全一致。
- 数据库只保存 BCrypt 哈希，不保存明文密码。

## 6. 前端调用顺序

```text
1. POST /api/password/account/verify
2. POST /api/password/sms-code
3. POST /api/password/confirm
4. 成功后弹窗提示“重置完成”，跳转回登录页
```

## 7. 安全要求

- 三个接口均为未登录可访问接口，但需要做参数校验和频率限制。
- 短信验证码只保存哈希，禁止保存明文验证码。
- `resetToken` 需要设置过期时间，重置成功后立即失效。
- 获取验证码需要 60 秒冷却，避免频繁发送。
- 修改密码时必须使用登录模块一致的 BCrypt 加密策略。

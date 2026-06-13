# 忘记密码 / 重置密码 功能设计

## 背景与目标

用户登录时可选择"忘记密码"，通过邮箱接收验证码后自行设置新密码。复用现有注册验证码体系（`VerificationCodeStore` + `EmailService` + JWT 登录体系）的约定与代码风格。

## 1. 验证码存储：按用途隔离

`src/main/java/com/aihedgefund/auth/VerificationCodeStore.java`

- 方法签名变更：
  - `generate(String email, String purpose)`
  - `verify(String email, String code, String purpose)`
- 内部 `ConcurrentHashMap` 的 key 由 `email.toLowerCase()` 改为 `email.toLowerCase() + ":" + purpose`，避免"注册验证码"与"重置密码验证码"互相覆盖。
- 新增常量：`VerificationPurpose`（简单字符串常量类即可），值：
  - `REGISTER = "register"`
  - `RESET_PASSWORD = "reset-password"`
- 现有调用方（`AuthService.sendVerificationCode` / `register`）改为传入 `VerificationPurpose.REGISTER`，行为保持不变。
- `cleanup()` 定时清理逻辑不变（基于 `expireAt`，与 key 结构无关）。

## 2. 数据访问层

`src/main/java/com/aihedgefund/mapper/UserMapper.java` + `src/main/resources/mapper/UserMapper.xml`

新增：

```java
/** 更新密码（同时更新 update_time） */
int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
```

```xml
<update id="updatePassword">
    UPDATE users
    SET password_hash = #{passwordHash},
        update_time = datetime('now')
    WHERE id = #{id}
      AND deleted = 0
</update>
```

> 注：现表结构字段名为 `updated_at`（非 `update_time`），SQL 中以实际表结构字段名 `updated_at` 为准；本设计不修改表结构。

## 3. 请求 DTO

`src/main/java/com/aihedgefund/model/req/`

- **找回密码请求**：直接复用现有 `SendCodeReq`（`email` 字段 + `@NotBlank` + `@Email` 校验），不新建文件。
- **新增 `ResetPasswordReq`**：
  - `email`：`@NotBlank @Email`
  - `code`：`@NotBlank`（6 位验证码）
  - `newPassword`：`@NotBlank @Size(min=6, max=64)`
  - `confirmPassword`：`@NotBlank`

## 4. EmailService

`src/main/java/com/aihedgefund/service/EmailService.java`

新增方法：

```java
public void sendPasswordResetCode(String toEmail, String code, int ttlMinutes)
```

- 结构与 `sendVerificationCode` 一致（同样的 `mailSender == null` 降级日志逻辑）
- 邮件文案区分用途，例如：
  > 您正在重置 {fromName} 账号密码。
  >
  > 验证码：{code}
  >
  > 验证码有效期 {ttlMinutes} 分钟，请勿将验证码告知他人。
  >
  > 如非本人操作，请忽略此邮件并检查账号安全。

## 5. AuthService 新增方法

`src/main/java/com/aihedgefund/service/AuthService.java`

```java
/** 发送密码重置验证码 */
public void sendPasswordResetCode(String email) {
    // 1. 校验用户是否存在，不存在抛 BizException("该邮箱未注册")
    // 2. codeStore.generate(email, VerificationPurpose.RESET_PASSWORD)
    // 3. emailService.sendPasswordResetCode(email, code, ttlMinutes)
}

/** 重置密码 */
@Transactional
public void resetPassword(ResetPasswordReq req) {
    // 1. newPassword != confirmPassword -> BizException("两次输入的密码不一致")
    // 2. codeStore.verify(email, code, RESET_PASSWORD) 失败
    //    -> BizException("验证码无效或已过期，请重新获取")
    // 3. 查询用户，不存在 -> BizException(404, "用户不存在")
    // 4. userMapper.updatePassword(id, passwordEncoder.encode(newPassword))
}
```

日志：每一步均输出 info/debug 日志（email、userId，不记录验证码和密码明文）。

## 6. Controller 接口

`src/main/java/com/aihedgefund/controller/AuthController.java`

```java
/** 发送密码重置验证码（公开接口） */
@PostMapping("/forgot-password")
public Map<String, String> forgotPassword(@RequestBody @Valid SendCodeReq req) {
    authService.sendPasswordResetCode(req.getEmail());
    return Map.of("message", "验证码已发送，请查收邮件");
}

/** 重置密码（公开接口） */
@PostMapping("/reset-password")
public Map<String, String> resetPassword(@RequestBody @Valid ResetPasswordReq req) {
    authService.resetPassword(req);
    return Map.of("message", "密码重置成功，请使用新密码登录");
}
```

## 7. 鉴权配置

`src/main/java/com/aihedgefund/auth/AuthWebConfig.java`

`excludePathPatterns` 新增：
- `/auth/forgot-password`
- `/auth/reset-password`

## 8. 前端

### `frontend/src/components/AuthModal.vue`

- 登录表单下方新增"忘记密码？"链接 -> `mode = 'forgot'`
- 新增 `mode === 'forgot'` 表单：
  - 邮箱输入 + "发送验证码"按钮（复用与注册一致的 60s 倒计时 `startCountdown`）
  - 验证码输入
  - 新密码 / 确认密码输入
  - 提交按钮"重置密码"
  - 底部"返回登录"链接 -> `mode = 'login'`
- 提交成功：`mode = 'login'`，登录表单上方显示成功提示"密码重置成功，请使用新密码登录"（复用 `loginError`/新增 `loginSuccess` 提示位）

### `frontend/src/stores/authStore.js`

新增方法（与 `register` 同样的 fetch + 错误处理风格）：

```js
async function forgotPassword(email)
async function resetPassword(email, code, newPassword, confirmPassword)
```

- 均为 `POST` 请求，失败时抛出 `Error(err.detail || ...)`
- 成功后**不**调用 `_setSession`（按设计约定，重置后需用户手动重新登录）

## 9. 错误场景一览

| 场景 | 异常/提示 |
|---|---|
| 找回密码时邮箱未注册 | `BizException("该邮箱未注册")` -> 400 |
| 发送验证码过于频繁 | 已有逻辑：`BizException("发送过于频繁，请 N 秒后重试")` -> 400 |
| 重置时验证码错误/过期/已用 | `BizException("验证码无效或已过期，请重新获取")` -> 400 |
| 两次新密码不一致 | `BizException("两次输入的密码不一致")` -> 400 |
| 重置时用户不存在（极端情况） | `BizException(404, "用户不存在")` -> 404 |

## 10. 测试计划

- **单测**
  - `VerificationCodeStoreTest`：同一邮箱不同 purpose 互不影响；同 purpose 校验/过期/已用逻辑保持正确
  - `AuthServiceTest`：新增 `sendPasswordResetCode`、`resetPassword` 用例，覆盖上表所有错误场景 + 成功路径
- **FT**
  - `AuthControllerTest` 补充 `/auth/forgot-password`、`/auth/reset-password` 端到端用例（含未鉴权可访问验证）
- **前端**
  - 手动验证：忘记密码 -> 控制台/邮件查看验证码（开发模式日志输出）-> 重置 -> 用新密码登录成功
- 同步更新 README 与 API 文档，补充两个新接口的请求/响应说明

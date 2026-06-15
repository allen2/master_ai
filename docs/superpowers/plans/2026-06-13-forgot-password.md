# 忘记密码 / 重置密码 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用户在登录时可选择"忘记密码"，通过邮箱验证码自行重置密码。

**Architecture:** 复用现有 `VerificationCodeStore`（增加 purpose 维度）+ `EmailService`（新增重置密码邮件模板）+ `AuthService`/`AuthController` 新增两个公开接口；前端在 `AuthModal.vue` 中新增 `forgot` 模式表单。

**Tech Stack:** Spring Boot 3 (jakarta) / MyBatis / SQLite / JUnit5 + AssertJ / Vue3 + Pinia / Vitest

**编译运行前提：** 本机默认 `JAVA_HOME` 指向 JDK 8，本项目需要 JDK 17。所有 `mvn` 命令需加上：

```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o ...
```

---

### Task 1: VerificationCodeStore 按用途（purpose）隔离验证码

**Files:**
- Create: `src/main/java/com/aihedgefund/auth/VerificationPurpose.java`
- Modify: `src/main/java/com/aihedgefund/auth/VerificationCodeStore.java`
- Modify: `src/main/java/com/aihedgefund/service/AuthService.java:52-58,71`（`sendVerificationCode` 和 `register` 中的 `codeStore` 调用）
- Test: `src/test/java/com/aihedgefund/auth/VerificationCodeStoreTest.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/aihedgefund/auth/VerificationCodeStoreTest.java`：

```java
package com.aihedgefund.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VerificationCodeStore 集成测试：验证按 purpose 隔离验证码。
 */
@SpringBootTest
class VerificationCodeStoreTest {

    @Autowired
    private VerificationCodeStore codeStore;

    /** 同一邮箱、不同 purpose 的验证码互不影响 */
    @Test
    void generate_differentPurposes_doNotCollide() {
        String email = "purpose-test-" + System.nanoTime() + "@example.com";

        String registerCode = codeStore.generate(email, VerificationPurpose.REGISTER);
        String resetCode = codeStore.generate(email, VerificationPurpose.RESET_PASSWORD);

        // register 验证码无法用于 reset-password 校验
        assertThat(codeStore.verify(email, registerCode, VerificationPurpose.RESET_PASSWORD)).isFalse();
        // reset-password 验证码可以正常校验通过
        assertThat(codeStore.verify(email, resetCode, VerificationPurpose.RESET_PASSWORD)).isTrue();
        // register 验证码仍然有效（未被 reset-password 的校验消费）
        assertThat(codeStore.verify(email, registerCode, VerificationPurpose.REGISTER)).isTrue();
    }

    /** 校验通过后验证码立即失效，不可重复使用 */
    @Test
    void verify_success_marksCodeAsUsed() {
        String email = "purpose-used-" + System.nanoTime() + "@example.com";
        String code = codeStore.generate(email, VerificationPurpose.RESET_PASSWORD);

        assertThat(codeStore.verify(email, code, VerificationPurpose.RESET_PASSWORD)).isTrue();
        assertThat(codeStore.verify(email, code, VerificationPurpose.RESET_PASSWORD)).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试确认失败（编译错误）**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o -Dtest=VerificationCodeStoreTest test
```
Expected: 编译失败（`generate(String, String)` / `verify(String, String, String)` 不存在）。

- [ ] **Step 3: 创建 VerificationPurpose 常量类**

创建 `src/main/java/com/aihedgefund/auth/VerificationPurpose.java`：

```java
package com.aihedgefund.auth;

/**
 * 邮箱验证码用途常量，用于 {@link VerificationCodeStore} 按用途隔离验证码。
 */
public final class VerificationPurpose {

    private VerificationPurpose() {
    }

    /** 注册验证码 */
    public static final String REGISTER = "register";

    /** 重置密码验证码 */
    public static final String RESET_PASSWORD = "reset-password";
}
```

- [ ] **Step 4: 修改 VerificationCodeStore，新增 purpose 参数**

修改 `src/main/java/com/aihedgefund/auth/VerificationCodeStore.java`：

将 `generate` 方法签名从 `generate(String email)` 改为：

```java
    /**
     * 生成并保存验证码。若距上次发送不足冷却时间，则抛出 BizException。
     *
     * @param email   目标邮箱（大小写不敏感）
     * @param purpose 验证码用途，见 {@link VerificationPurpose}
     * @return 生成的 6 位验证码
     */
    public String generate(String email, String purpose) {
        String key = buildKey(email, purpose);
        CodeEntry existing = store.get(key);
        if (existing != null && !existing.used()) {
            long cooldownMs = (long) resendCooldown * 1000;
            long sinceLastSend = System.currentTimeMillis() - existing.createdAt();
            if (sinceLastSend < cooldownMs) {
                long waitSec = (cooldownMs - sinceLastSend) / 1000 + 1;
                throw new BizException("发送过于频繁，请 " + waitSec + " 秒后重试");
            }
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        long now = System.currentTimeMillis();
        store.put(key, new CodeEntry(code, now + (long) ttlSeconds * 1000, now, false));
        log.debug("生成验证码, key={}, code={}, ttl={}s", key, code, ttlSeconds);
        return code;
    }
```

将 `verify` 方法签名从 `verify(String email, String code)` 改为：

```java
    /**
     * 校验验证码。校验成功后立即标记为已使用，防止重复消费。
     *
     * @param email   邮箱
     * @param code    用户输入的验证码
     * @param purpose 验证码用途，见 {@link VerificationPurpose}
     * @return true 表示验证通过
     */
    public boolean verify(String email, String code, String purpose) {
        String key = buildKey(email, purpose);
        CodeEntry entry = store.get(key);
        if (entry == null) {
            log.debug("验证码不存在, key={}", key);
            return false;
        }
        if (entry.used()) {
            log.debug("验证码已使用, key={}", key);
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt()) {
            log.debug("验证码已过期, key={}", key);
            store.remove(key);
            return false;
        }
        if (!entry.code().equals(code)) {
            log.debug("验证码错误, key={}", key);
            return false;
        }
        // 标记为已使用
        store.put(key, new CodeEntry(entry.code(), entry.expireAt(), entry.createdAt(), true));
        log.debug("验证码验证通过, key={}", key);
        return true;
    }
```

新增私有方法 `buildKey`（放在 `verify` 方法之后，`cleanup` 方法之前）：

```java
    /** 构造存储 key：邮箱（小写）+ 用途，避免不同用途的验证码互相覆盖 */
    private String buildKey(String email, String purpose) {
        return email.toLowerCase() + ":" + purpose;
    }
```

- [ ] **Step 5: 更新 AuthService 中现有的两处调用**

修改 `src/main/java/com/aihedgefund/service/AuthService.java`：

第 54 行 `sendVerificationCode` 方法内：
```java
        String code = codeStore.generate(email, VerificationPurpose.REGISTER);
```

第 71 行 `register` 方法内：
```java
        if (!codeStore.verify(req.getUsername(), req.getCode(), VerificationPurpose.REGISTER)) {
```

并在文件顶部 import 区域新增：
```java
import com.aihedgefund.auth.VerificationPurpose;
```
（紧跟在 `import com.aihedgefund.auth.VerificationCodeStore;` 之后）

- [ ] **Step 6: 运行测试确认通过**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o -Dtest=VerificationCodeStoreTest test
```
Expected: `Tests run: 2, Failures: 0, Errors: 0` / `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/aihedgefund/auth/VerificationPurpose.java \
        src/main/java/com/aihedgefund/auth/VerificationCodeStore.java \
        src/main/java/com/aihedgefund/service/AuthService.java \
        src/test/java/com/aihedgefund/auth/VerificationCodeStoreTest.java
git commit -m "feat: 验证码存储按用途(purpose)隔离，新增 VerificationPurpose 常量"
```

---

### Task 2: UserMapper 新增 updatePassword

**Files:**
- Modify: `src/main/java/com/aihedgefund/mapper/UserMapper.java`
- Modify: `src/main/resources/mapper/UserMapper.xml`
- Test: `src/test/java/com/aihedgefund/mapper/UserMapperTest.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/aihedgefund/mapper/UserMapperTest.java`：

```java
package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.UserDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserMapper 集成测试（内存 SQLite）。
 */
@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    /** updatePassword 应更新 password_hash 并保留其他字段不变 */
    @Test
    void updatePassword_updatesPasswordHash() {
        UserDO user = new UserDO();
        user.setUsername("update-pwd-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("old-hash");
        user.setNickname("测试用户");
        userMapper.insert(user);

        int affected = userMapper.updatePassword(user.getId(), "new-hash");
        assertThat(affected).isEqualTo(1);

        UserDO updated = userMapper.selectById(user.getId());
        assertThat(updated.getPasswordHash()).isEqualTo("new-hash");
        assertThat(updated.getUsername()).isEqualTo(user.getUsername());
    }
}
```

- [ ] **Step 2: 运行测试确认失败（编译错误）**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o -Dtest=UserMapperTest test
```
Expected: 编译失败（`updatePassword` 方法不存在）。

- [ ] **Step 3: UserMapper 接口新增方法**

修改 `src/main/java/com/aihedgefund/mapper/UserMapper.java`，在 `insert` 方法之后新增：

```java
    /** 更新密码（同时更新 updated_at） */
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
```

- [ ] **Step 4: UserMapper.xml 新增 SQL**

修改 `src/main/resources/mapper/UserMapper.xml`，在 `<insert id="insert">` 之后新增：

```xml
    <update id="updatePassword">
        UPDATE users
        SET password_hash = #{passwordHash},
            updated_at = datetime('now')
        WHERE id = #{id}
          AND deleted = 0
    </update>
```

- [ ] **Step 5: 运行测试确认通过**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o -Dtest=UserMapperTest test
```
Expected: `Tests run: 1, Failures: 0, Errors: 0` / `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/aihedgefund/mapper/UserMapper.java \
        src/main/resources/mapper/UserMapper.xml \
        src/test/java/com/aihedgefund/mapper/UserMapperTest.java
git commit -m "feat: UserMapper 新增 updatePassword，支持重置密码"
```

---

### Task 3: EmailService 新增 sendPasswordResetCode

**Files:**
- Modify: `src/main/java/com/aihedgefund/service/EmailService.java`
- Test: `src/test/java/com/aihedgefund/service/EmailServiceTest.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/aihedgefund/service/EmailServiceTest.java`：

```java
package com.aihedgefund.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * EmailService 单元测试：未配置 mailSender 时（开发模式）应降级为日志输出，不抛异常。
 */
class EmailServiceTest {

    private final EmailService emailService = new EmailService();

    /** mailSender 为 null（未配置邮件服务）时，发送密码重置验证码不抛异常 */
    @Test
    void sendPasswordResetCode_mailSenderNotConfigured_doesNotThrow() {
        assertThatCode(() -> emailService.sendPasswordResetCode("user@example.com", "123456", 5))
                .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: 运行测试确认失败（编译错误）**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o -Dtest=EmailServiceTest test
```
Expected: 编译失败（`sendPasswordResetCode` 方法不存在）。

- [ ] **Step 3: EmailService 新增方法**

修改 `src/main/java/com/aihedgefund/service/EmailService.java`，在 `sendVerificationCode` 方法之后新增：

```java
    /**
     * 发送密码重置验证码。
     *
     * @param toEmail    收件人邮箱
     * @param code       6 位验证码
     * @param ttlMinutes 有效分钟数（用于邮件正文提示）
     */
    public void sendPasswordResetCode(String toEmail, String code, int ttlMinutes) {
        if (mailSender == null || fromAddress.isBlank()) {
            // 未配置邮件服务：仅打印日志，方便开发环境调试
            log.warn("邮件服务未配置，重置密码验证码不会通过邮件发送。[开发模式] email={} code={}", toEmail, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromAddress + ">");
            message.setTo(toEmail);
            message.setSubject("【" + fromName + "】重置密码验证码");
            message.setText(buildResetPasswordText(code, ttlMinutes));
            mailSender.send(message);
            log.info("重置密码验证码邮件发送成功, to={}", toEmail);
        } catch (MailException e) {
            log.error("重置密码验证码邮件发送失败, to={}, error={}", toEmail, e.getMessage());
            throw new BizException("邮件发送失败：" + e.getMessage());
        }
    }

    private String buildResetPasswordText(String code, int ttlMinutes) {
        return "您正在重置 " + fromName + " 账号密码。\n\n"
                + "验证码：" + code + "\n\n"
                + "验证码有效期 " + ttlMinutes + " 分钟，请勿将验证码告知他人。\n\n"
                + "如非本人操作，请忽略此邮件并检查账号安全。";
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o -Dtest=EmailServiceTest test
```
Expected: `Tests run: 1, Failures: 0, Errors: 0` / `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aihedgefund/service/EmailService.java \
        src/test/java/com/aihedgefund/service/EmailServiceTest.java
git commit -m "feat: EmailService 新增重置密码验证码邮件模板"
```

---

### Task 4: ResetPasswordReq + AuthService.sendPasswordResetCode / resetPassword

**Files:**
- Create: `src/main/java/com/aihedgefund/model/req/ResetPasswordReq.java`
- Modify: `src/main/java/com/aihedgefund/service/AuthService.java`
- Test: `src/test/java/com/aihedgefund/service/AuthServiceTest.java`

- [ ] **Step 1: 创建 ResetPasswordReq**

创建 `src/main/java/com/aihedgefund/model/req/ResetPasswordReq.java`：

```java
package com.aihedgefund.model.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置密码入参
 */
public class ResetPasswordReq {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "请输入有效的邮箱地址")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需为 6-64 个字符")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
```

- [ ] **Step 2: 写失败的测试**

创建 `src/test/java/com/aihedgefund/service/AuthServiceTest.java`：

```java
package com.aihedgefund.service;

import com.aihedgefund.auth.VerificationCodeStore;
import com.aihedgefund.auth.VerificationPurpose;
import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.UserMapper;
import com.aihedgefund.model.DO.UserDO;
import com.aihedgefund.model.req.ResetPasswordReq;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AuthService 找回密码相关方法集成测试。
 */
@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VerificationCodeStore codeStore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserDO createUser(String email) {
        UserDO user = new UserDO();
        user.setUsername(email);
        user.setPasswordHash(passwordEncoder.encode("old-password"));
        user.setNickname("测试用户");
        userMapper.insert(user);
        return user;
    }

    private ResetPasswordReq buildReq(String email, String code, String newPassword, String confirmPassword) {
        ResetPasswordReq req = new ResetPasswordReq();
        req.setEmail(email);
        req.setCode(code);
        req.setNewPassword(newPassword);
        req.setConfirmPassword(confirmPassword);
        return req;
    }

    /** 邮箱未注册时发送重置验证码应抛出业务异常 */
    @Test
    void sendPasswordResetCode_emailNotRegistered_throwsBizException() {
        String email = "not-registered-" + System.nanoTime() + "@example.com";

        assertThatThrownBy(() -> authService.sendPasswordResetCode(email))
                .isInstanceOf(BizException.class)
                .hasMessage("该邮箱未注册");
    }

    /** 邮箱已注册时发送重置验证码不抛异常 */
    @Test
    void sendPasswordResetCode_registeredEmail_doesNotThrow() {
        UserDO user = createUser("send-reset-" + System.nanoTime() + "@example.com");

        authService.sendPasswordResetCode(user.getUsername());
        // 不抛异常即视为通过；验证码已写入 codeStore（用于下一条用例的场景）
        assertThat(codeStore.verify(user.getUsername(), "000000", VerificationPurpose.RESET_PASSWORD)).isFalse();
    }

    /** 两次输入的新密码不一致应抛出业务异常 */
    @Test
    void resetPassword_passwordMismatch_throwsBizException() {
        ReqContext ctx = new ReqContext("mismatch-" + System.nanoTime() + "@example.com");
        ResetPasswordReq req = buildReq(ctx.email, "123456", "newpass1", "newpass2");

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BizException.class)
                .hasMessage("两次输入的密码不一致");
    }

    /** 验证码错误或过期应抛出业务异常 */
    @Test
    void resetPassword_invalidCode_throwsBizException() {
        String email = "invalid-code-" + System.nanoTime() + "@example.com";
        ResetPasswordReq req = buildReq(email, "000000", "newpassword", "newpassword");

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BizException.class)
                .hasMessage("验证码无效或已过期，请重新获取");
    }

    /** 验证码有效但用户不存在（极端情况）应返回 404 业务异常 */
    @Test
    void resetPassword_userNotFound_throwsNotFoundException() {
        String email = "ghost-" + System.nanoTime() + "@example.com";
        String code = codeStore.generate(email, VerificationPurpose.RESET_PASSWORD);
        ResetPasswordReq req = buildReq(email, code, "newpassword", "newpassword");

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("code", 404);
    }

    /** 成功重置密码：密码被更新为新密码的哈希 */
    @Test
    void resetPassword_success_updatesPasswordHash() {
        UserDO user = createUser("reset-success-" + System.nanoTime() + "@example.com");
        String code = codeStore.generate(user.getUsername(), VerificationPurpose.RESET_PASSWORD);
        ResetPasswordReq req = buildReq(user.getUsername(), code, "brand-new-pass", "brand-new-pass");

        authService.resetPassword(req);

        UserDO updated = userMapper.selectById(user.getId());
        assertThat(passwordEncoder.matches("brand-new-pass", updated.getPasswordHash())).isTrue();
    }

    /** 辅助类：仅用于生成唯一邮箱（密码不一致场景下不需要真实用户/验证码） */
    private static class ReqContext {
        final String email;
        ReqContext(String email) { this.email = email; }
    }
}
```

- [ ] **Step 3: 运行测试确认失败（编译错误）**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o -Dtest=AuthServiceTest test
```
Expected: 编译失败（`sendPasswordResetCode` / `resetPassword` 方法不存在）。

- [ ] **Step 4: AuthService 新增方法**

修改 `src/main/java/com/aihedgefund/service/AuthService.java`：

在文件顶部 import 区域新增：
```java
import com.aihedgefund.model.req.ResetPasswordReq;
```
（按字母顺序放在 `import com.aihedgefund.model.req.RegisterReq;` 之后）

在 `register` 方法之后、`login` 方法之前新增：

```java
    /**
     * 发送密码重置验证码。
     *
     * @param email 目标邮箱
     */
    public void sendPasswordResetCode(String email) {
        log.info("发送密码重置验证码请求, email={}", email);

        UserDO user = userMapper.selectByUsername(email);
        if (user == null) {
            throw new BizException("该邮箱未注册");
        }

        String code = codeStore.generate(email, VerificationPurpose.RESET_PASSWORD);
        int ttlMinutes = codeTtlSeconds / 60;
        emailService.sendPasswordResetCode(email, code, ttlMinutes);
        log.info("密码重置验证码已生成并发送, email={}", email);
    }

    /**
     * 重置密码（需先通过邮箱验证码校验）。
     *
     * @param req 重置密码入参
     */
    @Transactional
    public void resetPassword(ResetPasswordReq req) {
        log.info("重置密码请求, email={}", req.getEmail());

        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BizException("两次输入的密码不一致");
        }

        if (!codeStore.verify(req.getEmail(), req.getCode(), VerificationPurpose.RESET_PASSWORD)) {
            throw new BizException("验证码无效或已过期，请重新获取");
        }

        UserDO user = userMapper.selectByUsername(req.getEmail());
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        userMapper.updatePassword(user.getId(), passwordEncoder.encode(req.getNewPassword()));
        log.info("密码重置成功, userId={}, email={}", user.getId(), req.getEmail());
    }
```

- [ ] **Step 5: 运行测试确认通过**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o -Dtest=AuthServiceTest test
```
Expected: `Tests run: 6, Failures: 0, Errors: 0` / `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/aihedgefund/model/req/ResetPasswordReq.java \
        src/main/java/com/aihedgefund/service/AuthService.java \
        src/test/java/com/aihedgefund/service/AuthServiceTest.java
git commit -m "feat: AuthService 新增找回密码与重置密码业务逻辑"
```

---

### Task 5: AuthController 新增接口 + AuthWebConfig 公开路径

**Files:**
- Modify: `src/main/java/com/aihedgefund/controller/AuthController.java`
- Modify: `src/main/java/com/aihedgefund/auth/AuthWebConfig.java`
- Test: `src/test/java/com/aihedgefund/controller/AuthControllerTest.java`

- [ ] **Step 1: 写失败的测试**

创建 `src/test/java/com/aihedgefund/controller/AuthControllerTest.java`：

```java
package com.aihedgefund.controller;

import com.aihedgefund.auth.VerificationCodeStore;
import com.aihedgefund.auth.VerificationPurpose;
import com.aihedgefund.mapper.UserMapper;
import com.aihedgefund.model.DO.UserDO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 找回密码 / 重置密码接口集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VerificationCodeStore codeStore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDO createUser(String email) {
        UserDO user = new UserDO();
        user.setUsername(email);
        user.setPasswordHash(passwordEncoder.encode("old-password"));
        user.setNickname("测试用户");
        userMapper.insert(user);
        return user;
    }

    /** 邮箱未注册时调用 /auth/forgot-password 返回 400 */
    @Test
    void forgotPassword_emailNotRegistered_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "not-exist-" + System.nanoTime() + "@example.com"));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("该邮箱未注册"));
    }

    /** 邮箱已注册时调用 /auth/forgot-password 返回成功消息 */
    @Test
    void forgotPassword_registeredEmail_returns200() throws Exception {
        UserDO user = createUser("forgot-ok-" + System.nanoTime() + "@example.com");
        String body = objectMapper.writeValueAsString(Map.of("email", user.getUsername()));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("验证码已发送，请查收邮件"));
    }

    /** 两次新密码不一致时调用 /auth/reset-password 返回 400 */
    @Test
    void resetPassword_passwordMismatch_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "mismatch-" + System.nanoTime() + "@example.com",
                "code", "123456",
                "newPassword", "password1",
                "confirmPassword", "password2"
        ));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("两次输入的密码不一致"));
    }

    /** 验证码错误时调用 /auth/reset-password 返回 400 */
    @Test
    void resetPassword_invalidCode_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "invalid-" + System.nanoTime() + "@example.com",
                "code", "000000",
                "newPassword", "newpassword",
                "confirmPassword", "newpassword"
        ));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("验证码无效或已过期，请重新获取"));
    }

    /** 重置密码成功后，可使用新密码登录 */
    @Test
    void resetPassword_success_thenLoginWithNewPassword() throws Exception {
        UserDO user = createUser("reset-flow-" + System.nanoTime() + "@example.com");
        String code = codeStore.generate(user.getUsername(), VerificationPurpose.RESET_PASSWORD);

        String resetBody = objectMapper.writeValueAsString(Map.of(
                "email", user.getUsername(),
                "code", code,
                "newPassword", "brand-new-pass",
                "confirmPassword", "brand-new-pass"
        ));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("密码重置成功，请使用新密码登录"));

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", user.getUsername(),
                "password", "brand-new-pass"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o -Dtest=AuthControllerTest test
```
Expected: 失败（404 Not Found，接口尚未存在）。

- [ ] **Step 3: AuthController 新增接口**

修改 `src/main/java/com/aihedgefund/controller/AuthController.java`，在文件顶部 import 区域新增：

```java
import com.aihedgefund.model.req.ResetPasswordReq;
```
（按字母顺序放在 `import com.aihedgefund.model.req.RegisterReq;` 之后）

在 `register` 方法之后新增：

```java
    /**
     * 发送密码重置验证码（公开接口）。
     */
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody @Valid SendCodeReq req) {
        authService.sendPasswordResetCode(req.getEmail());
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送，请查收邮件");
        return result;
    }

    /**
     * 重置密码（公开接口，需先调用 /auth/forgot-password 获取验证码）。
     */
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestBody @Valid ResetPasswordReq req) {
        authService.resetPassword(req);
        Map<String, String> result = new HashMap<>();
        result.put("message", "密码重置成功，请使用新密码登录");
        return result;
    }
```

- [ ] **Step 4: AuthWebConfig 新增公开路径**

修改 `src/main/java/com/aihedgefund/auth/AuthWebConfig.java`，`excludePathPatterns` 列表中新增两项：

```java
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/auth/send-code",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/hedge-fund/analysts",
                        "/admin/**",
                        "/health",
                        "/error"
                );
```

- [ ] **Step 5: 运行测试确认通过**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o -Dtest=AuthControllerTest test
```
Expected: `Tests run: 5, Failures: 0, Errors: 0` / `BUILD SUCCESS`

- [ ] **Step 6: 运行完整后端测试套件，确认未引入回归**

Run:
```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o test
```
Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/aihedgefund/controller/AuthController.java \
        src/main/java/com/aihedgefund/auth/AuthWebConfig.java \
        src/test/java/com/aihedgefund/controller/AuthControllerTest.java
git commit -m "feat: 新增 /auth/forgot-password 和 /auth/reset-password 接口"
```

---

### Task 6: 前端 authStore 新增 forgotPassword / resetPassword

**Files:**
- Modify: `frontend/src/stores/authStore.js`
- Test: `frontend/src/stores/__tests__/authStore.test.js`

- [ ] **Step 1: 写失败的测试**

修改 `frontend/src/stores/__tests__/authStore.test.js`，在 `logout 清除 token/user 和 localStorage` 测试之后、文件末尾 `})` 之前新增：

```javascript
  it('forgotPassword 成功时返回 message', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ message: '验证码已发送，请查收邮件' })
    })

    const auth = useAuthStore()
    const result = await auth.forgotPassword('user@example.com')

    expect(result.message).toBe('验证码已发送，请查收邮件')
    expect(globalThis.fetch).toHaveBeenCalledWith('/auth/forgot-password', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ email: 'user@example.com' })
    }))
  })

  it('forgotPassword 失败时抛出包含 detail 的错误', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: () => Promise.resolve({ detail: '该邮箱未注册' })
    })

    const auth = useAuthStore()
    await expect(auth.forgotPassword('not-exist@example.com')).rejects.toThrow('该邮箱未注册')
  })

  it('resetPassword 成功时返回 message，且不写入登录状态', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ message: '密码重置成功，请使用新密码登录' })
    })

    const auth = useAuthStore()
    const result = await auth.resetPassword('user@example.com', '123456', 'newpass', 'newpass')

    expect(result.message).toBe('密码重置成功，请使用新密码登录')
    expect(auth.isLoggedIn).toBe(false)
    expect(globalThis.fetch).toHaveBeenCalledWith('/auth/reset-password', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ email: 'user@example.com', code: '123456', newPassword: 'newpass', confirmPassword: 'newpass' })
    }))
  })

  it('resetPassword 失败时抛出包含 detail 的错误', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: () => Promise.resolve({ detail: '验证码无效或已过期，请重新获取' })
    })

    const auth = useAuthStore()
    await expect(auth.resetPassword('user@example.com', '000000', 'newpass', 'newpass'))
        .rejects.toThrow('验证码无效或已过期，请重新获取')
  })
```

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
cd frontend && npx vitest run src/stores/__tests__/authStore.test.js
```
Expected: 失败（`auth.forgotPassword is not a function`）

- [ ] **Step 3: authStore.js 新增方法**

修改 `frontend/src/stores/authStore.js`，在 `register` 方法之后新增：

```javascript
  /**
   * 发送密码重置验证码
   * @param {string} email
   */
  async function forgotPassword(email) {
    const res = await fetch('/auth/forgot-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.detail || `发送失败 (${res.status})`)
    }
    return res.json()
  }

  /**
   * 重置密码
   * @param {string} email
   * @param {string} code      邮箱验证码
   * @param {string} newPassword
   * @param {string} confirmPassword
   */
  async function resetPassword(email, code, newPassword, confirmPassword) {
    const res = await fetch('/auth/reset-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, code, newPassword, confirmPassword })
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.detail || `重置失败 (${res.status})`)
    }
    return res.json()
  }
```

并将 `return` 语句改为：

```javascript
  return { token, user, isLoggedIn, login, register, logout, forgotPassword, resetPassword }
```

- [ ] **Step 4: 运行测试确认通过**

Run:
```bash
cd frontend && npx vitest run src/stores/__tests__/authStore.test.js
```
Expected: 全部通过（9 个用例）

- [ ] **Step 5: Commit**

```bash
git add frontend/src/stores/authStore.js frontend/src/stores/__tests__/authStore.test.js
git commit -m "feat: authStore 新增 forgotPassword 和 resetPassword 方法"
```

---

### Task 7: AuthModal.vue 新增"忘记密码"模式

**Files:**
- Modify: `frontend/src/components/AuthModal.vue`

- [ ] **Step 1: 修改弹窗标题，支持 forgot 模式**

修改 `frontend/src/components/AuthModal.vue` 第 13 行：

```html
      <h2 class="auth-title">{{ mode === 'login' ? '登录' : mode === 'register' ? '注册账号' : '找回密码' }}</h2>
```

- [ ] **Step 2: 登录表单新增"忘记密码？"链接和成功提示**

修改登录表单（第 17-32 行），将：

```html
    <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="auth-form">
      <div class="form-field">
        <label>邮箱</label>
        <input v-model="loginUsername" type="email" placeholder="请输入邮箱地址" autocomplete="email" required />
      </div>
      <div class="form-field">
        <label>密码</label>
        <input v-model="loginPassword" type="password" placeholder="请输入密码" autocomplete="current-password" required />
      </div>

      <div v-if="loginError" class="auth-error">{{ loginError }}</div>

      <button type="submit" class="auth-btn" :disabled="loginLoading">
        {{ loginLoading ? '登录中…' : '登录' }}
      </button>
    </form>
```

替换为：

```html
    <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="auth-form">
      <div class="form-field">
        <label>邮箱</label>
        <input v-model="loginUsername" type="email" placeholder="请输入邮箱地址" autocomplete="email" required />
      </div>
      <div class="form-field">
        <label>密码</label>
        <input v-model="loginPassword" type="password" placeholder="请输入密码" autocomplete="current-password" required />
      </div>

      <div class="auth-forgot-link">
        <a href="javascript:void(0)" @click="switchMode('forgot')">忘记密码？</a>
      </div>

      <div v-if="loginError" class="auth-error">{{ loginError }}</div>
      <div v-if="loginSuccess" class="auth-success">{{ loginSuccess }}</div>

      <button type="submit" class="auth-btn" :disabled="loginLoading">
        {{ loginLoading ? '登录中…' : '登录' }}
      </button>
    </form>
```

- [ ] **Step 3: 注册表单条件改为 `mode === 'register'`，并新增找回密码表单**

修改注册表单的开始标签（原第 35 行）：

```html
    <!-- 注册表单 -->
    <form v-else @submit.prevent="handleRegister" class="auth-form">
```

改为：

```html
    <!-- 注册表单 -->
    <form v-else-if="mode === 'register'" @submit.prevent="handleRegister" class="auth-form">
```

并在注册表单的 `</form>`（原第 94 行）之后新增找回密码表单：

```html
    </form>

    <!-- 找回密码表单 -->
    <form v-else @submit.prevent="handleResetPassword" class="auth-form">
      <div class="form-field">
        <label>邮箱</label>
        <div class="email-row">
          <input
            v-model="forgotEmail"
            type="email"
            placeholder="请输入注册时使用的邮箱地址"
            autocomplete="email"
            required
            maxlength="100"
            :disabled="resetCodeSent && forgotCountdown > 0"
          />
          <button
            type="button"
            class="send-btn"
            :disabled="sendingResetCode || forgotCountdown > 0 || !forgotEmail"
            @click="handleSendResetCode"
          >
            {{ forgotCountdown > 0 ? `${forgotCountdown}s 后重发` : (sendingResetCode ? '发送中…' : '发送验证码') }}
          </button>
        </div>
      </div>

      <div class="form-field">
        <label>验证码 <span class="hint">（6 位数字，有效期 5 分钟）</span></label>
        <input
          v-model="forgotCode"
          type="text"
          inputmode="numeric"
          placeholder="请输入邮件中的验证码"
          autocomplete="one-time-code"
          required
          maxlength="6"
          pattern="\d{6}"
        />
      </div>

      <div class="form-field">
        <label>新密码 <span class="hint">（6-64 个字符）</span></label>
        <input v-model="forgotNewPassword" type="password" placeholder="请输入新密码" autocomplete="new-password" required minlength="6" maxlength="64" />
      </div>

      <div class="form-field">
        <label>确认新密码</label>
        <input v-model="forgotConfirmPassword" type="password" placeholder="再次输入新密码" autocomplete="new-password" required />
      </div>

      <div v-if="forgotError" class="auth-error">{{ forgotError }}</div>
      <div v-if="forgotSuccess" class="auth-success">{{ forgotSuccess }}</div>

      <button type="submit" class="auth-btn" :disabled="forgotLoading">
        {{ forgotLoading ? '提交中…' : '重置密码' }}
      </button>
    </form>
```

- [ ] **Step 4: 底部链接区域支持三种模式**

修改第 96-103 行：

```html
    <div class="auth-link">
      <template v-if="mode === 'login'">
        还没有账号？<a href="javascript:void(0)" @click="switchMode('register')">立即注册</a>
      </template>
      <template v-else>
        已有账号？<a href="javascript:void(0)" @click="switchMode('login')">去登录</a>
      </template>
    </div>
```

替换为：

```html
    <div class="auth-link">
      <template v-if="mode === 'login'">
        还没有账号？<a href="javascript:void(0)" @click="switchMode('register')">立即注册</a>
      </template>
      <template v-else-if="mode === 'register'">
        已有账号？<a href="javascript:void(0)" @click="switchMode('login')">去登录</a>
      </template>
      <template v-else>
        <a href="javascript:void(0)" @click="switchMode('login')">返回登录</a>
      </template>
    </div>
```

- [ ] **Step 5: switchMode 和 watch(visible) 重置新增字段**

修改 `switchMode` 函数（原第 130-135 行）：

```javascript
function switchMode(target) {
  mode.value = target
  loginError.value     = ''
  loginSuccess.value   = ''
  registerError.value  = ''
  registerSuccess.value = ''
  forgotError.value    = ''
  forgotSuccess.value  = ''
}
```

修改 `watch(() => props.visible, ...)`（原第 121-128 行）：

```javascript
watch(() => props.visible, (val) => {
  if (val) {
    mode.value = props.initialMode
    loginError.value   = ''
    loginSuccess.value = ''
    registerError.value   = ''
    registerSuccess.value = ''
    forgotError.value     = ''
    forgotSuccess.value   = ''
  }
})
```

- [ ] **Step 6: 新增登录成功提示状态**

修改登录相关 ref 定义（原第 138-141 行）：

```javascript
// 登录
const loginUsername = ref('')
const loginPassword = ref('')
const loginLoading  = ref(false)
const loginError    = ref('')
const loginSuccess  = ref('')
```

- [ ] **Step 7: 新增找回密码相关状态和方法**

在 `onUnmounted(() => clearInterval(countdownTimer))`（原第 235 行）之前新增：

```javascript
// 找回密码
const forgotEmail           = ref('')
const forgotCode            = ref('')
const forgotNewPassword     = ref('')
const forgotConfirmPassword = ref('')
const forgotLoading         = ref(false)
const sendingResetCode      = ref(false)
const resetCodeSent         = ref(false)
const forgotCountdown       = ref(0)
const forgotError           = ref('')
const forgotSuccess         = ref('')

let forgotCountdownTimer = null

function startForgotCountdown(seconds) {
  forgotCountdown.value = seconds
  clearInterval(forgotCountdownTimer)
  forgotCountdownTimer = setInterval(() => {
    forgotCountdown.value--
    if (forgotCountdown.value <= 0) {
      clearInterval(forgotCountdownTimer)
      forgotCountdownTimer = null
    }
  }, 1000)
}

async function handleSendResetCode() {
  forgotError.value   = ''
  forgotSuccess.value = ''
  if (!forgotEmail.value) {
    forgotError.value = '请先填写邮箱'
    return
  }
  sendingResetCode.value = true
  try {
    await auth.forgotPassword(forgotEmail.value)
    resetCodeSent.value = true
    forgotSuccess.value = '验证码已发送，请查收邮件'
    startForgotCountdown(60)
  } catch (e) {
    forgotError.value = e.message
  } finally {
    sendingResetCode.value = false
  }
}

async function handleResetPassword() {
  forgotError.value   = ''
  forgotSuccess.value = ''
  if (!resetCodeSent.value) {
    forgotError.value = '请先获取验证码'
    return
  }
  if (forgotNewPassword.value !== forgotConfirmPassword.value) {
    forgotError.value = '两次输入的密码不一致'
    return
  }
  forgotLoading.value = true
  try {
    await auth.resetPassword(forgotEmail.value, forgotCode.value, forgotNewPassword.value, forgotConfirmPassword.value)
    switchMode('login')
    loginSuccess.value = '密码重置成功，请使用新密码登录'
  } catch (e) {
    forgotError.value = e.message
  } finally {
    forgotLoading.value = false
  }
}
```

并修改 `onUnmounted`：

```javascript
onUnmounted(() => {
  clearInterval(countdownTimer)
  clearInterval(forgotCountdownTimer)
})
```

- [ ] **Step 8: 新增 CSS 样式**

在 `<style scoped>` 块中、`.auth-link` 规则（原第 351-356 行）之前新增：

```css
.auth-forgot-link {
  text-align: right;
  font-size: 13px;
  margin-top: -8px;
}
.auth-forgot-link a {
  color: #2563eb;
  text-decoration: none;
}
.auth-forgot-link a:hover {
  text-decoration: underline;
}
```

- [ ] **Step 9: 手动验证（开发服务器）**

Run:
```bash
cd frontend && npm run dev
```

在浏览器中：
1. 打开登录弹窗，点击"忘记密码？" -> 应切换到"找回密码"表单，标题显示"找回密码"
2. 输入一个未注册的邮箱，点击"发送验证码" -> 应显示错误"该邮箱未注册"
3. 用已注册邮箱点击"发送验证码" -> 显示"验证码已发送，请查收邮件"，按钮进入 60s 倒计时；后端日志（未配置邮件服务时）应以 `WARN` 打印验证码
4. 输入验证码 + 新密码 + 确认密码，点击"重置密码" -> 切回登录表单，显示"密码重置成功，请使用新密码登录"
5. 使用新密码登录 -> 成功
6. 点击"返回登录" -> 切回登录表单，无报错

Stop server with Ctrl+C after verification.

- [ ] **Step 10: Commit**

```bash
git add frontend/src/components/AuthModal.vue
git commit -m "feat: AuthModal 新增找回密码模式"
```

---

### Task 8: 更新 README 文档

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 在"用户认证"章节新增"找回密码"小节**

修改 `README.md`，在第 144 行（`### 登录` 标题）之前新增：

```markdown
### 找回密码

忘记密码时，可通过邮箱验证码自行重置密码：

```bash
# 1. 发送重置密码验证码（需为已注册邮箱）
curl -X POST http://localhost:8000/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'

# 2. 使用验证码重置密码
curl -X POST http://localhost:8000/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "code": "123456", "newPassword": "new_password", "confirmPassword": "new_password"}'
```

重置成功后请使用新密码重新登录。

```

- [ ] **Step 2: 更新"后端 API"表格**

修改 `README.md` 第 230-233 行：

```markdown
| `POST` | `/auth/send-code` | 发送邮箱验证码 |
| `POST` | `/auth/register` | 注册（需验证码） |
| `POST` | `/auth/login` | 登录，返回 JWT token |
| `GET` | `/auth/me` | 获取当前用户信息 |
```

替换为：

```markdown
| `POST` | `/auth/send-code` | 发送邮箱验证码（注册用） |
| `POST` | `/auth/register` | 注册（需验证码） |
| `POST` | `/auth/login` | 登录，返回 JWT token |
| `GET` | `/auth/me` | 获取当前用户信息 |
| `POST` | `/auth/forgot-password` | 发送密码重置验证码 |
| `POST` | `/auth/reset-password` | 使用验证码重置密码 |
```

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: 补充找回密码/重置密码接口说明"
```

---

## 完成后整体验证

- [ ] **Step 1: 运行完整后端测试**

```bash
JAVA_HOME="/d/jdk-17.0.19" PATH="/d/jdk-17.0.19/bin:$PATH" mvn -o test
```
Expected: `BUILD SUCCESS`

- [ ] **Step 2: 运行完整前端测试**

```bash
cd frontend && npx vitest run
```
Expected: 全部通过

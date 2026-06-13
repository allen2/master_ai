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
        String email = "mismatch-" + System.nanoTime() + "@example.com";
        ResetPasswordReq req = buildReq(email, "123456", "newpass1", "newpass2");

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
}

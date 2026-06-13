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

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

package com.aihedgefund.service;

import com.aihedgefund.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务。
 *
 * <p>若 {@code spring.mail.username} 未配置，则降级为仅打印日志（方便本地开发调试）。
 * 生产环境请务必配置 MAIL_USERNAME / MAIL_PASSWORD 环境变量。</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /** 允许 mailSender 为 null（未配置时 Spring Boot 不创建 Bean） */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${verify-code.from-name:金木班}")
    private String fromName;

    /**
     * 发送邮箱验证码。
     *
     * @param toEmail   收件人邮箱
     * @param code      6 位验证码
     * @param ttlMinutes 有效分钟数（用于邮件正文提示）
     */
    public void sendVerificationCode(String toEmail, String code, int ttlMinutes) {
        if (mailSender == null || fromAddress.isBlank()) {
            // 未配置邮件服务：仅打印日志，方便开发环境调试
            log.warn("邮件服务未配置，验证码不会通过邮件发送。[开发模式] email={} code={}", toEmail, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromAddress + ">");
            message.setTo(toEmail);
            message.setSubject("【" + fromName + "】邮箱验证码");
            message.setText(buildEmailText(code, ttlMinutes));
            mailSender.send(message);
            log.info("验证码邮件发送成功, to={}", toEmail);
        } catch (MailException e) {
            log.error("验证码邮件发送失败, to={}, error={}", toEmail, e.getMessage());
            throw new BizException("邮件发送失败：" + e.getMessage());
        }
    }

    private String buildEmailText(String code, int ttlMinutes) {
        return "您正在注册 " + fromName + " 账号。\n\n"
                + "验证码：" + code + "\n\n"
                + "验证码有效期 " + ttlMinutes + " 分钟，请勿将验证码告知他人。\n\n"
                + "如非本人操作，请忽略此邮件。";
    }

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
}

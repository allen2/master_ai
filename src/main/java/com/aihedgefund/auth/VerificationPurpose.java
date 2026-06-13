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

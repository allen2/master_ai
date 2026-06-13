package com.aihedgefund.controller;

import com.aihedgefund.auth.AuthUser;
import com.aihedgefund.auth.UserContext;
import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.model.DO.UserDO;
import com.aihedgefund.model.req.LoginReq;
import com.aihedgefund.model.req.RegisterReq;
import com.aihedgefund.model.req.ResetPasswordReq;
import com.aihedgefund.model.req.SendCodeReq;
import com.aihedgefund.model.resp.AuthResp;
import com.aihedgefund.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口：注册、登录、发送验证码、当前用户。
 * /auth/login、/auth/register、/auth/send-code 为公开路径。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 向邮箱发送注册验证码（公开接口）。
     */
    @PostMapping("/send-code")
    public Map<String, String> sendCode(@RequestBody @Valid SendCodeReq req) {
        authService.sendVerificationCode(req.getEmail());
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送，请查收邮件");
        return result;
    }

    /**
     * 注册（需先调用 /auth/send-code 获取验证码）。
     */
    @PostMapping("/register")
    public AuthResp register(@RequestBody @Valid RegisterReq req) {
        return authService.register(req);
    }

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

    /**
     * 登录。
     */
    @PostMapping("/login")
    public AuthResp login(@RequestBody @Valid LoginReq req) {
        return authService.login(req);
    }

    /**
     * 获取当前登录用户信息（需带 token）。
     */
    @GetMapping("/me")
    public Map<String, Object> me() {
        AuthUser current = UserContext.get();
        if (current == null) {
            throw new BizException(401, "未登录");
        }
        UserDO user = authService.getById(current.getUserId());
        Map<String, Object> body = new HashMap<>();
        body.put("userId", user.getId());
        body.put("username", user.getUsername());
        body.put("nickname", user.getNickname());
        return body;
    }
}

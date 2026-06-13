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

package com.aihedgefund.controller;

import com.aihedgefund.mapper.UserMapper;
import com.aihedgefund.model.DO.UserDO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminController 集成测试：验证 /admin/coins/grant 的鉴权及按 userId / email 发放金币。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    private static final String ADMIN_TOKEN = "mumu-admin-2024";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 缺少 X-Admin-Token 时应返回 401。
     */
    @Test
    void grantCoins_missingAdminToken_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("userId", 1, "amount", 10));

        mockMvc.perform(post("/admin/coins/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 按 userId 发放金币：成功返回用户信息和发放后余额。
     */
    @Test
    void grantCoins_byUserId_success() throws Exception {
        UserDO user = createUser("admin-grant-userid@example.com");

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", user.getId(),
                "amount", 5,
                "reason", "测试发放"
        ));

        mockMvc.perform(post("/admin/coins/grant")
                        .header(ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.amount").value(5));
    }

    /**
     * 按注册邮箱（即用户名）发放金币：成功返回对应用户信息。
     */
    @Test
    void grantCoins_byEmail_success() throws Exception {
        UserDO user = createUser("admin-grant-email@example.com");

        String body = objectMapper.writeValueAsString(Map.of(
                "email", user.getUsername(),
                "amount", 8,
                "reason", "按邮箱发放"
        ));

        mockMvc.perform(post("/admin/coins/grant")
                        .header(ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.amount").value(8));
    }

    /**
     * 邮箱不存在时应返回 404。
     */
    @Test
    void grantCoins_emailNotFound_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "not-exist-" + System.nanoTime() + "@example.com",
                "amount", 1
        ));

        mockMvc.perform(post("/admin/coins/grant")
                        .header(ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    /**
     * userId 和 email 都未提供时应返回 400。
     */
    @Test
    void grantCoins_neitherUserIdNorEmail_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("amount", 1));

        mockMvc.perform(post("/admin/coins/grant")
                        .header(ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * 创建一个测试用户并返回其 DO（含自增 id）。
     */
    private UserDO createUser(String email) {
        UserDO user = new UserDO();
        user.setUsername(email);
        user.setPasswordHash("test-hash");
        user.setNickname("测试用户");
        userMapper.insert(user);
        return user;
    }
}

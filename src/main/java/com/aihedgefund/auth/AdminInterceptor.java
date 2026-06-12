package com.aihedgefund.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 管理员接口拦截器：校验请求头 X-Admin-Token。
 * 仅拦截 /admin/** 路径（由 AuthWebConfig 注册）。
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminInterceptor.class);
    private static final String HEADER_ADMIN_TOKEN = "X-Admin-Token";

    @Value("${admin.token:mumu-admin-2024}")
    private String adminToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        String token = request.getHeader(HEADER_ADMIN_TOKEN);
        if (adminToken.equals(token)) {
            return true;
        }
        log.warn("管理员鉴权失败: {} {}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"detail\":\"管理员令牌无效\"}");
        return false;
    }
}

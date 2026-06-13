package com.aihedgefund.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册鉴权拦截器，配置公开路径和管理员路径。
 */
@Configuration
public class AuthWebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;

    public AuthWebConfig(AuthInterceptor authInterceptor, AdminInterceptor adminInterceptor) {
        this.authInterceptor = authInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 鉴权：覆盖所有路径，排除公开接口和管理员接口
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

        // 管理员令牌鉴权：仅覆盖 /admin/**
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
    }
}

package com.aihedgefund.support;

import com.aihedgefund.auth.JwtUtil;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 测试支持：为自动配置的 MockMvc 默认附带合法 JWT 的 Authorization 头，
 * 使受鉴权拦截器保护的接口测试无需逐个请求手动加 token。
 *
 * <p>用法：在 @SpringBootTest + @AutoConfigureMockMvc 的控制器测试上
 * 添加 {@code @Import(MockMvcAuthConfig.class)}。</p>
 */
@TestConfiguration
public class MockMvcAuthConfig {

    @Bean
    public MockMvcBuilderCustomizer authHeaderCustomizer(JwtUtil jwtUtil) {
        String token = jwtUtil.generateToken(1L, "tester");
        return builder -> builder.defaultRequest(
                get("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}

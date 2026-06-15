package com.aihedgefund.common.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 模拟静态资源（如 .css）处理失败场景：响应已被预设为 text/css 且未提交，
     * 期望 handleGeneral 重置响应后正常返回 JSON 错误体，避免转换器找不到匹配 Content-Type。
     */
    @Test
    void handleGeneral_resetsUncommittedResponse_beforeWritingJsonBody() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(false);

        ResponseEntity<Map<String, String>> result = handler.handleGeneral(response, new RuntimeException("boom"));

        verify(response).reset();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody()).containsEntry("detail", "Internal server error: boom");
    }

    /**
     * 响应已提交时不能再调用 reset()（会抛出 IllegalStateException），
     * 期望 handleGeneral 跳过 reset 并仍返回错误体。
     */
    @Test
    void handleGeneral_doesNotResetCommittedResponse() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(true);

        ResponseEntity<Map<String, String>> result = handler.handleGeneral(response, new RuntimeException("boom"));

        verify(response, never()).reset();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

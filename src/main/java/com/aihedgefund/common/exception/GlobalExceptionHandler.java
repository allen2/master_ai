package com.aihedgefund.common.exception;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理，返回 FastAPI 兼容的 {"detail": "..."} 格式
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, String>> handleBiz(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        HttpStatus status;
        if (e.getCode() == 404) {
            status = HttpStatus.NOT_FOUND;
        } else if (e.getCode() == 401) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (e.getCode() == 402) {
            status = HttpStatus.PAYMENT_REQUIRED;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        Map<String, String> body = new HashMap<>();
        body.put("detail", e.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", detail);
        Map<String, String> body = new HashMap<>();
        body.put("detail", detail);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResource(NoResourceFoundException e) {
        log.debug("静态资源未找到: {}", e.getResourcePath());
        Map<String, String> body = new HashMap<>();
        body.put("detail", "Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException e) {
        // 客户端提前断开连接（如关闭页面/切换路由），属于正常现象，仅记录 debug 日志
        log.debug("客户端断开连接: {}", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(HttpServletResponse response, Exception e) {
        log.error("未预期异常: ", e);
        // 静态资源（如 .css/.js）处理失败时，响应可能已被预设为非 JSON 的 Content-Type，
        // 此时需先重置响应，避免后续 JSON 转换器找不到匹配的 Content-Type 而再次报错
        if (!response.isCommitted()) {
            response.reset();
        }
        Map<String, String> body = new HashMap<>();
        body.put("detail", "Internal server error: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

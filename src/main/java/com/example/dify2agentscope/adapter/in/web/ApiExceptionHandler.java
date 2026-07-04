package com.example.dify2agentscope.adapter.in.web;

import com.example.dify2agentscope.domain.security.WorkflowSecurityException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局 API 异常处理器 —— 统一捕获并返回标准错误响应。
 * Global API exception handler — catches exceptions and returns standardized error responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 处理请求参数校验失败异常。
     * Handle method argument validation failures.
     *
     * @param exception 校验异常 / validation exception
     * @return 400 错误响应 / 400 bad request response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "invalid_request",
                "message", exception.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(error -> error.getField() + " " + error.getDefaultMessage())
                        .orElse("invalid request")));
    }

    /**
     * 处理工作流安全权限异常。
     * Handle workflow security / permission exceptions.
     *
     * @param exception 安全异常 / security exception
     * @return 403 禁止访问响应 / 403 forbidden response
     */
    @ExceptionHandler(WorkflowSecurityException.class)
    public ResponseEntity<Map<String, Object>> security(WorkflowSecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "error", "permission_denied",
                "message", exception.getMessage()));
    }

    /**
     * 处理其他未捕获的通用异常。
     * Handle any other unhandled generic exceptions.
     *
     * @param exception 通用异常 / generic exception
     * @return 500 内部错误响应 / 500 internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "internal_error",
                "message", exception.getMessage()));
    }
}

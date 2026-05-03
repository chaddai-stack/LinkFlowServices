package com.linkflow.api.common.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 业务 API 异常
 *
 * 服务层用它表达可预期业务错误，由全局异常处理器转换成统一 响应信封。
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public ApiException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        // 保存不可变副本，防止调用方修改已抛出的异常上下文。
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}

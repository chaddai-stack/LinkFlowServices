package com.linkflow.api.common.response;

import java.util.Map;

/**
 * API 错误体
 *
 * code 供前端做分支处理，message 供用户展示，details 保存字段级或上下文错误。
 */
public record ApiError(
        String code,
        String message,
        Map<String, Object> details
) {
    public ApiError {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}

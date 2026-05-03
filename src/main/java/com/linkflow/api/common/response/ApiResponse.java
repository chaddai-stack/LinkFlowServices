package com.linkflow.api.common.response;

import com.linkflow.api.common.web.RequestIdContext;

import java.util.Map;

/**
 * 统一 JSON 响应信封
 *
 * 所有 JSON API 使用 data/error/meta/request_id 结构；图片、重定向等非 JSON 响应不使用该信封。
 */
public record ApiResponse<T>(
        String request_id,
        T data,
        ApiError error,
        Map<String, Object> meta
) {
    public ApiResponse {
        meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(RequestIdContext.getRequestId(), data, null, Map.of());
    }

    public static <T> ApiResponse<T> success(T data, Map<String, Object> meta) {
        return new ApiResponse<>(RequestIdContext.getRequestId(), data, null, meta);
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(RequestIdContext.getRequestId(), null, error, Map.of());
    }
}

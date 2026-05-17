package com.linkflow.api.link.dto.response;

import java.util.List;

/**
 * 已创建链接批量操作汇总。
 */
public record BulkLinkActionResponse(
        int total,
        int succeeded,
        int failed,
        List<BulkLinkActionItemResponse> items
) {
    public static BulkLinkActionResponse from(List<BulkLinkActionItemResponse> items) {
        int succeeded = (int) items.stream()
                .filter(item -> "succeeded".equals(item.status()))
                .count();
        return new BulkLinkActionResponse(
                items.size(),
                succeeded,
                items.size() - succeeded,
                items
        );
    }
}

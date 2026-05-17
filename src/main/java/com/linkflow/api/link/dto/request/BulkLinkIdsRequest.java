package com.linkflow.api.link.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * 已创建链接的批量操作请求。
 *
 * 最小闭环只接收 link_id 列表，不引入复杂筛选条件，避免批量操作影响不可见数据。
 */
public record BulkLinkIdsRequest(
        @JsonProperty("link_ids")
        @NotEmpty(message = "link_ids must not be empty")
        @Size(max = 100, message = "link_ids size must be <= 100")
        List<@NotNull(message = "link_id must not be null") UUID> linkIds
) {
}

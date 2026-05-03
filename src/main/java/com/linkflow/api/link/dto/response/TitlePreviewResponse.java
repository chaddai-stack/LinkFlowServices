package com.linkflow.api.link.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 标题预览响应
 *
 * source 表示 title 来源，例如 爬虫 或 兜底。
 */
public record TitlePreviewResponse(
        @JsonProperty("long_url")
        String longUrl,
        String title,
        String source
) {
}

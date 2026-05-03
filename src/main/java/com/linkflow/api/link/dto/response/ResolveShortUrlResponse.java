package com.linkflow.api.link.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 早期短链解析响应
 */
public record ResolveShortUrlResponse(
        @JsonProperty("back_half")
        String backHalf,
        String longUrl
) {
}

package com.linkflow.api.link.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * 可读 back_half 推荐请求。
 *
 * long_url 和 title 至少传一个；跨字段校验交给服务层处理。
 */
public record BackHalfRecommendationRequest(
        @JsonProperty("long_url")
        @Size(max = 2048, message = "long_url length must be <= 2048")
        @Pattern(regexp = "(?i)^https?://.+$", message = "long_url must start with http:// or https://")
        @URL(message = "long_url must be a valid URL")
        String longUrl,

        @Size(max = 300, message = "title length must be <= 300")
        String title,

        @Min(value = 1, message = "limit must be >= 1")
        @Max(value = 20, message = "limit must be <= 20")
        Integer limit
) {
}

package com.linkflow.api.link.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * 标题预览请求
 */
public record TitlePreviewRequest(
        @JsonProperty("long_url")
        @NotBlank(message = "long_url must not be blank")
        @Size(max = 2048, message = "long_url length must be <= 2048")
        @Pattern(regexp = "(?i)^https?://.+$", message = "long_url must start with http:// or https://")
        @URL(message = "long_url must be a valid URL")
        String longUrl
) {
}

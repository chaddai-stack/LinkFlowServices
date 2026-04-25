package com.linkflow.api.link.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record UpdateLinkRequest(
        @JsonProperty("long_url")
        @Size(max = 2048, message = "long_url length must be <= 2048")
        @Pattern(regexp = "(?i)^https?://.+$", message = "long_url must start with http:// or https://")
        @URL(message = "long_url must be a valid URL")
        String longUrl,

        @Size(max = 300, message = "title length must be <= 300")
        @Pattern(regexp = ".*\\S.*", message = "title must not be blank")
        String title,

        @JsonProperty("custom_slug")
        @Size(min = 3, max = 80, message = "custom_slug length must be between 3 and 80")
        @Pattern(
                regexp = "^[a-zA-Z0-9][a-zA-Z0-9_-]{2,79}$",
                message = "custom_slug must match ^[a-zA-Z0-9][a-zA-Z0-9_-]{2,79}$"
        )
        String customSlug,

        @Size(max = 80, message = "channel length must be <= 80")
        String channel,

        @JsonProperty("expires_at")
        OffsetDateTime expiresAt,

        List<String> tags,

        Map<String, Object> metadata
) {
}

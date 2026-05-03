package com.linkflow.api.risk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 黑名单写入请求
 */
public record BlacklistRequest(
        @NotBlank(message = "value must not be blank")
        @Size(max = 2048, message = "value length must be <= 2048")
        String value,

        @Size(max = 1000, message = "reason length must be <= 1000")
        String reason
) {
}

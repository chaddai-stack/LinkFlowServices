package com.linkflow.api.link.dto.response;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 可读 back_half 推荐响应。
 */
public record BackHalfRecommendationResponse(
        List<BackHalfRecommendationItem> suggestions
) {
    /**
     * 单个 back_half 推荐项。
     */
    public record BackHalfRecommendationItem(
            @JsonProperty("back_half")
            String backHalf,
            String source,
            boolean available
    ) {
        /**
         * 保留 slug 字段，兼容旧前端。
         */
        @JsonGetter("slug")
        public String slug() {
            return backHalf;
        }
    }
}

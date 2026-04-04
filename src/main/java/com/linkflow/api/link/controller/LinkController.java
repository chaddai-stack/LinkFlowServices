package com.linkflow.api.link.controller;

import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.link.dto.LinkSummaryResponse;
import com.linkflow.api.link.service.LinkQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/links")
public class LinkController {

    private final LinkQueryService linkQueryService;

    public LinkController(LinkQueryService linkQueryService) {
        this.linkQueryService = linkQueryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LinkSummaryResponse>>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<LinkSummaryResponse> result = linkQueryService.list(page, size);
        Map<String, Object> meta = Map.of(
                "page", Map.of(
                        "page", page,
                        "size", size,
                        "total_elements", result.getTotalElements(),
                        "total_pages", result.getTotalPages(),
                        "has_next", result.hasNext()
                )
        );
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), meta));
    }

    @GetMapping("/{link_id}")
    public ResponseEntity<ApiResponse<LinkSummaryResponse>> detail(@PathVariable("link_id") UUID linkId) {
        return ResponseEntity.ok(ApiResponse.success(linkQueryService.getById(linkId)));
    }
}

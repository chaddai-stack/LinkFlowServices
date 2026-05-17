package com.linkflow.api.link.controller;

import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.link.dto.request.BulkCreateLinksRequest;
import com.linkflow.api.link.dto.response.BulkLinkJobResponse;
import com.linkflow.api.link.service.BulkLinkService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/links/bulk")
@SecurityRequirement(name = "bearerAuth")
public class BulkLinkController {

    private final BulkLinkService bulkLinkService;

    public BulkLinkController(BulkLinkService bulkLinkService) {
        this.bulkLinkService = bulkLinkService;
    }

    /**
     * 批量创建短链，并逐条执行检测和分类。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BulkLinkJobResponse>> create(
            @Valid @RequestBody BulkCreateLinksRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(bulkLinkService.create(request)));
    }

    /**
     * 查询批量任务结果。
     */
    @GetMapping("/{job_id}")
    public ResponseEntity<ApiResponse<BulkLinkJobResponse>> get(@PathVariable("job_id") UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(bulkLinkService.get(jobId)));
    }
}

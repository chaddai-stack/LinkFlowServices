package com.linkflow.api.link.controller;

import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.link.dto.request.BulkLinkIdsRequest;
import com.linkflow.api.link.dto.request.BulkUpdateLinkStatusRequest;
import com.linkflow.api.link.dto.response.BulkLinkActionResponse;
import com.linkflow.api.link.service.BulkLinkActionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 已创建链接的批量操作接口。
 *
 * 与 BulkLinkController 的“批量创建任务”分开命名，避免创建流程和管理流程混在一起。
 */
@RestController
@RequestMapping("/api/v1/links/bulk")
@SecurityRequirement(name = "bearerAuth")
public class BulkLinkActionController {

    private final BulkLinkActionService bulkLinkActionService;

    public BulkLinkActionController(BulkLinkActionService bulkLinkActionService) {
        this.bulkLinkActionService = bulkLinkActionService;
    }

    @PatchMapping("/status")
    public ResponseEntity<ApiResponse<BulkLinkActionResponse>> updateStatus(
            @Valid @RequestBody BulkUpdateLinkStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                bulkLinkActionService.updateStatus(request.linkIds(), request.status())
        ));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<BulkLinkActionResponse>> delete(
            @Valid @RequestBody BulkLinkIdsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(bulkLinkActionService.delete(request.linkIds())));
    }
}

package com.linkflow.api.link.controller;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.link.dto.CreateLinkRequest;
import com.linkflow.api.link.dto.LinkSummaryResponse;
import com.linkflow.api.link.dto.UpdateLinkRequest;
import com.linkflow.api.link.dto.UpdateLinkStatusRequest;
import com.linkflow.api.link.service.QrCodeService;
import com.linkflow.api.link.service.LinkCommandService;
import com.linkflow.api.link.service.LinkQueryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/links")
@SecurityRequirement(name = "bearerAuth")
public class LinkController {

    private final LinkQueryService linkQueryService;
    private final LinkCommandService linkCommandService;
    private final QrCodeService qrCodeService;
    private final String publicBaseUrl;

    public LinkController(
            LinkQueryService linkQueryService,
            LinkCommandService linkCommandService,
            QrCodeService qrCodeService,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.linkQueryService = linkQueryService;
        this.linkCommandService = linkCommandService;
        this.qrCodeService = qrCodeService;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LinkSummaryResponse>> create(@Valid @RequestBody CreateLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(LinkSummaryResponse.from(linkCommandService.create(request))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LinkSummaryResponse>>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "created_at,desc") String sort
    ) {
        if (page < 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of("page", "must be greater than or equal to 1")
            );
        }

        if (size < 1 || size > 100) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of("size", "must be less than or equal to 100")
            );
        }

        Page<LinkSummaryResponse> result = linkQueryService.list(page, size, status, search, sort);
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

    @GetMapping(value = "/{link_id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(
            @PathVariable("link_id") UUID linkId,
            @RequestParam(defaultValue = "png") @Pattern(regexp = "^png$", message = "format must be png") String format,
            @RequestParam(defaultValue = "512") @Min(128) @Max(2048) int size
    ) {
        var mapping = linkQueryService.getMappingById(linkId);
        byte[] png = qrCodeService.generatePng(publicBaseUrl + "/r/" + mapping.getSlug(), size);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @PatchMapping("/{link_id}")
    public ResponseEntity<ApiResponse<LinkSummaryResponse>> update(
            @PathVariable("link_id") UUID linkId,
            @Valid @RequestBody UpdateLinkRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(LinkSummaryResponse.from(linkCommandService.update(linkId, request))));
    }

    @PatchMapping("/{link_id}/status")
    public ResponseEntity<ApiResponse<LinkSummaryResponse>> updateStatus(
            @PathVariable("link_id") UUID linkId,
            @Valid @RequestBody UpdateLinkStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(LinkSummaryResponse.from(linkCommandService.updateStatus(linkId, request))));
    }

    @DeleteMapping("/{link_id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("link_id") UUID linkId) {
        linkCommandService.delete(linkId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

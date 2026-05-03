package com.linkflow.api.link.controller;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.request.BackHalfRecommendationRequest;
import com.linkflow.api.link.dto.response.BackHalfRecommendationResponse;
import com.linkflow.api.link.dto.request.CreateLinkRequest;
import com.linkflow.api.link.dto.response.LinkSummaryResponse;
import com.linkflow.api.link.dto.response.QrCodeAssetResponse;
import com.linkflow.api.link.dto.request.TitlePreviewRequest;
import com.linkflow.api.link.dto.response.TitlePreviewResponse;
import com.linkflow.api.link.dto.request.UpdateLinkRequest;
import com.linkflow.api.link.dto.request.UpdateLinkStatusRequest;
import com.linkflow.api.link.service.BackHalfRecommendationService;
import com.linkflow.api.link.service.LinkTitleCrawlerService;
import com.linkflow.api.link.service.QrCodeAssetService;
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
    private final LinkTitleCrawlerService linkTitleCrawlerService;
    private final BackHalfRecommendationService backHalfRecommendationService;
    private final QrCodeAssetService qrCodeAssetService;
    private final QrCodeService qrCodeService;
    private final String publicBaseUrl;

    public LinkController(
            LinkQueryService linkQueryService,
            LinkCommandService linkCommandService,
            LinkTitleCrawlerService linkTitleCrawlerService,
            BackHalfRecommendationService backHalfRecommendationService,
            QrCodeAssetService qrCodeAssetService,
            QrCodeService qrCodeService,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.linkQueryService = linkQueryService;
        this.linkCommandService = linkCommandService;
        this.linkTitleCrawlerService = linkTitleCrawlerService;
        this.backHalfRecommendationService = backHalfRecommendationService;
        this.qrCodeAssetService = qrCodeAssetService;
        this.qrCodeService = qrCodeService;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    /**
     * 创建正式 LinkFlow 短链
     *
     * 控制器只负责请求校验和响应包装，业务规则交给 {@link LinkCommandService#create(CreateLinkRequest)}。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LinkSummaryResponse>> create(@Valid @RequestBody CreateLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(LinkSummaryResponse.from(linkCommandService.create(request), publicBaseUrl)));
    }

    /**
     * 预览页面标题
     *
     * 优先用爬虫抓取 HTML 标题；失败时返回兜底标题，避免创建流程被外部网页阻塞。
     */
    @PostMapping("/title-preview")
    public ResponseEntity<ApiResponse<TitlePreviewResponse>> titlePreview(
            @Valid @RequestBody TitlePreviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                linkTitleCrawlerService.fetchTitle(request.longUrl())
                        .orElseGet(() -> fallbackTitlePreview(request))
        ));
    }

    /**
     * 推荐可用 back-half
     *
     * 推荐逻辑由规则候选和可选 LLM 候选组成，最终都会经过规范化与数据库占用检查。
     */
    @PostMapping({"/back-half-recommendations", "/slug-recommendations"})
    public ResponseEntity<ApiResponse<BackHalfRecommendationResponse>> backHalfRecommendations(
            @Valid @RequestBody BackHalfRecommendationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(backHalfRecommendationService.recommend(request)));
    }

    /**
     * 分页查询短链
     *
     * 分页元数据放在响应信封的 meta.page 中；排序字段由服务层做白名单映射。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LinkSummaryResponse>>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "created_at,desc") String sort
    ) {
        validatePageRequest(page, size);
        Page<LinkSummaryResponse> result = linkQueryService.list(page, size, status, search, sort);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), pageMeta(page, size, result)));
    }

    /**
     * 查询短链详情
     */
    @GetMapping("/{link_id}")
    public ResponseEntity<ApiResponse<LinkSummaryResponse>> detail(@PathVariable("link_id") UUID linkId) {
        return ResponseEntity.ok(ApiResponse.success(linkQueryService.getById(linkId)));
    }

    /**
     * 直接返回二维码 PNG
     *
     * 该接口只在本地生成 PNG，不依赖 S3；图片二进制响应不使用 JSON 响应信封。
     */
    @GetMapping(value = "/{link_id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(
            @PathVariable("link_id") UUID linkId,
            @RequestParam(defaultValue = "png") @Pattern(regexp = "^png$", message = "format must be png") String format,
            @RequestParam(defaultValue = "512") @Min(128) @Max(2048) int size
    ) {
        byte[] png = qrCodeService.generatePng(publicShortUrl(linkId), size);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    /**
     * 获取或创建二维码云端资产
     *
     * S3 未启用时返回 data=null；这表示“未配置”，不是业务错误。
     */
    @GetMapping("/{link_id}/qrcode/asset")
    public ResponseEntity<ApiResponse<QrCodeAssetResponse>> qrCodeAsset(
            @PathVariable("link_id") UUID linkId,
            @RequestParam(defaultValue = "png") @Pattern(regexp = "^png$", message = "format must be png") String format,
            @RequestParam(defaultValue = "512") @Min(128) @Max(2048) int size
    ) {
        var mapping = linkMapping(linkId);
        return ResponseEntity.ok(ApiResponse.success(
                qrCodeAssetService.getOrCreateAsset(mapping, publicShortUrl(mapping), size)
                        .orElse(null)
        ));
    }

    /**
     * 更新短链详情
     *
     * 未传字段保持当前值；custom_back_half 变化时才检查唯一性。
     */
    @PatchMapping("/{link_id}")
    public ResponseEntity<ApiResponse<LinkSummaryResponse>> update(
            @PathVariable("link_id") UUID linkId,
            @Valid @RequestBody UpdateLinkRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(LinkSummaryResponse.from(linkCommandService.update(linkId, request), publicBaseUrl)));
    }

    /**
     * 更新短链状态
     */
    @PatchMapping("/{link_id}/status")
    public ResponseEntity<ApiResponse<LinkSummaryResponse>> updateStatus(
            @PathVariable("link_id") UUID linkId,
            @Valid @RequestBody UpdateLinkStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(LinkSummaryResponse.from(linkCommandService.updateStatus(linkId, request), publicBaseUrl)));
    }

    /**
     * 删除短链
     */
    @DeleteMapping("/{link_id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("link_id") UUID linkId) {
        linkCommandService.delete(linkId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private TitlePreviewResponse fallbackTitlePreview(TitlePreviewRequest request) {
        return new TitlePreviewResponse(
                request.longUrl(),
                linkTitleCrawlerService.resolveTitle(request.longUrl(), null),
                "fallback"
        );
    }

    private Map<String, Object> pageMeta(int page, int size, Page<?> result) {
        return Map.of(
                "page", Map.of(
                        "page", page,
                        "size", size,
                        "total_elements", result.getTotalElements(),
                        "total_pages", result.getTotalPages(),
                        "has_next", result.hasNext()
                )
        );
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1) {
            throw validationError("page", "must be greater than or equal to 1");
        }

        if (size < 1 || size > 100) {
            throw validationError("size", "must be less than or equal to 100");
        }
    }

    private ApiException validationError(String field, String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed.",
                Map.of(field, message)
        );
    }

    private String publicShortUrl(UUID linkId) {
        return publicShortUrl(linkMapping(linkId));
    }

    private String publicShortUrl(UrlMapping mapping) {
        return publicBaseUrl + "/" + mapping.getBackHalf();
    }

    private UrlMapping linkMapping(UUID linkId) {
        return linkQueryService.getMappingById(linkId);
    }
}

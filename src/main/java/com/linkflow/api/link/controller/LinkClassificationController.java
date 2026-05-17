package com.linkflow.api.link.controller;

import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.link.dto.response.LinkClassificationResponse;
import com.linkflow.api.link.service.LinkClassificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/links")
@SecurityRequirement(name = "bearerAuth")
public class LinkClassificationController {

    private final LinkClassificationService classificationService;

    public LinkClassificationController(LinkClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    /**
     * 为链接生成或刷新分类。
     * 当前使用规则分类，后续可在 service 中追加 Hugging Face 增强层。
     */
    @PostMapping("/{link_id}/classification")
    public ResponseEntity<ApiResponse<LinkClassificationResponse>> classify(
            @PathVariable("link_id") UUID linkId
    ) {
        return ResponseEntity.ok(ApiResponse.success(classificationService.classify(linkId)));
    }

    /**
     * 查询已生成的链接分类。
     */
    @GetMapping("/{link_id}/classification")
    public ResponseEntity<ApiResponse<LinkClassificationResponse>> get(
            @PathVariable("link_id") UUID linkId
    ) {
        return ResponseEntity.ok(ApiResponse.success(classificationService.get(linkId)));
    }
}

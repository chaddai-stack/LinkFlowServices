package com.linkflow.api.link.controller;

import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.link.dto.CreateLinkRequest;
import com.linkflow.api.link.dto.CreateLinkResponse;
import com.linkflow.api.link.dto.CreateShortUrlRequest;
import com.linkflow.api.link.dto.CreateShortUrlResponse;
import com.linkflow.api.link.service.UrlCodecService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class UrlController {

    private final UrlCodecService urlCodecService;

    public UrlController(UrlCodecService urlCodecService) {
        this.urlCodecService = urlCodecService;
    }

    @PostMapping("/api/short-urls")
    public CreateShortUrlResponse create(@Valid @RequestBody CreateShortUrlRequest request) {
        var mapping = urlCodecService.createOrGet(request.longUrl());
        return new CreateShortUrlResponse(
                mapping.getSlug(),
                "/api/short-urls/" + mapping.getSlug(),
                mapping.getLongUrl()
        );
    }

    @GetMapping("/api/short-urls/{slug}")
    public ResponseEntity<Void> shortToLong(@PathVariable String slug) {
        var mapping = urlCodecService.resolve(slug);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(mapping.getLongUrl()))
                .build();
    }

    @PostMapping("/api/v1/links")
    public ResponseEntity<ApiResponse<CreateLinkResponse>> createLink(@Valid @RequestBody CreateLinkRequest request) {
        var mapping = urlCodecService.createOrGet(request.longUrl());
        CreateLinkResponse response = new CreateLinkResponse(
                mapping.getSlug(),
                "/r/" + mapping.getSlug(),
                mapping.getLongUrl(),
                mapping.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/r/{slug}")
    public ResponseEntity<Void> redirect(@PathVariable String slug) {
        var mapping = urlCodecService.resolve(slug);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(mapping.getLongUrl()))
                .build();
    }
}

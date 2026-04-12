package com.linkflow.api.link.controller;

import com.linkflow.api.link.dto.CreateShortUrlRequest;
import com.linkflow.api.link.dto.CreateShortUrlResponse;
import com.linkflow.api.link.service.UrlCodecService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class UrlController {

    private final UrlCodecService urlCodecService;
    private final String publicBaseUrl;

    public UrlController(
            UrlCodecService urlCodecService,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.urlCodecService = urlCodecService;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @PostMapping("/api/short-urls")
    public CreateShortUrlResponse create(@Valid @RequestBody CreateShortUrlRequest request) {
        var mapping = urlCodecService.createOrGet(request.longUrl());
        return new CreateShortUrlResponse(
                mapping.getSlug(),
                absoluteUrl("/api/short-urls/" + mapping.getSlug()),
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

    @GetMapping("/r/{slug}")
    public ResponseEntity<Void> redirect(@PathVariable String slug) {
        var mapping = urlCodecService.resolve(slug);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(mapping.getLongUrl()))
                .build();
    }

    private String absoluteUrl(String path) {
        return publicBaseUrl + path;
    }
}

package com.linkflow.api.link.controller;

import com.linkflow.api.link.service.LinkRedirectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class LinkRedirectController {

    private final LinkRedirectService linkRedirectService;

    public LinkRedirectController(LinkRedirectService linkRedirectService) {
        this.linkRedirectService = linkRedirectService;
    }

    /**
     * 新版公开短链跳转入口
     */
    @GetMapping("/r/{back_half}")
    public ResponseEntity<Void> redirect(@PathVariable("back_half") String backHalf) {
        return redirectTo(backHalf);
    }

    /**
     * 根路径短链跳转
     *
     * 正则限制 back-half 形态，避免吞掉普通静态资源路径或其他 API 路径。
     */
    @GetMapping("/{back_half:[a-zA-Z0-9][a-zA-Z0-9_-]{2,79}}")
    public ResponseEntity<Void> rootRedirect(@PathVariable("back_half") String backHalf) {
        return redirectTo(backHalf);
    }

    private ResponseEntity<Void> redirectTo(String backHalf) {
        var mapping = linkRedirectService.resolve(backHalf);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(mapping.getLongUrl()))
                .build();
    }
}

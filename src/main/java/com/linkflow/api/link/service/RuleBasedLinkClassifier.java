package com.linkflow.api.link.service;

import com.linkflow.api.link.domain.UrlMapping;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@Service
public class RuleBasedLinkClassifier {

    /**
     * 规则分类底座。
     *
     * 当前先用域名、路径、标题、渠道做轻量分类，保证无外部模型时也能产出结果。
     * 后续接 Hugging Face 时，可以把模型结果作为增强信号再合并。
     */
    public ClassificationResult classify(UrlMapping link) {
        String haystack = String.join(" ",
                safe(link.getLongUrl()),
                safe(link.getTitle()),
                safe(link.getChannel()),
                hostOf(link.getLongUrl())
        ).toLowerCase(Locale.ROOT);

        if (containsAny(haystack, "github", "gitlab", "stackoverflow", "developer", "docs", "api", "code", "repo")) {
            return result("developer", 0.86, "developer_resource", "rule_keyword_match");
        }
        if (containsAny(haystack, "bank", "finance", "crypto", "coin", "invest", "pay", "payment", "wallet")) {
            return result("finance", 0.82, "finance_keyword", "rule_keyword_match");
        }
        if (containsAny(haystack, "shop", "store", "sale", "deal", "buy", "cart", "promo", "commerce")) {
            return result("shopping", 0.80, "commerce_keyword", "rule_keyword_match");
        }
        if (containsAny(haystack, "news", "article", "blog", "press", "medium", "substack")) {
            return result("news", 0.78, "content_keyword", "rule_keyword_match");
        }
        if (containsAny(haystack, "facebook", "instagram", "twitter", "x.com", "tiktok", "reddit", "linkedin", "wechat")) {
            return result("social", 0.78, "social_domain_or_keyword", "rule_keyword_match");
        }

        return result("unknown", 0.35, "no_rule_match", "rule_fallback");
    }

    private ClassificationResult result(String category, double confidence, String... labels) {
        return new ClassificationResult(category, confidence, List.of(labels), "rules");
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String hostOf(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host;
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

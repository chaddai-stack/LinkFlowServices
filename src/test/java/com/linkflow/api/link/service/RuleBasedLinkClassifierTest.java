package com.linkflow.api.link.service;

import com.linkflow.api.link.domain.UrlMapping;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedLinkClassifierTest {

    private final RuleBasedLinkClassifier classifier = new RuleBasedLinkClassifier();

    @Test
    void classifyDeveloperLinkFromDomain() {
        UrlMapping link = new UrlMapping(
                "https://github.com/example/repo",
                "repo",
                "Example repository",
                "docs",
                null
        );

        ClassificationResult result = classifier.classify(link);

        assertEquals("developer", result.category());
        assertTrue(result.confidence() > 0.8);
    }

    @Test
    void classifyShoppingLinkFromTitle() {
        UrlMapping link = new UrlMapping(
                "https://example.com/campaign",
                "sale",
                "Spring sale deal",
                "email",
                null
        );

        ClassificationResult result = classifier.classify(link);

        assertEquals("shopping", result.category());
    }

    @Test
    void classifyUnknownWhenNoRuleMatches() {
        UrlMapping link = new UrlMapping(
                "https://example.com/page",
                "page",
                "Plain page",
                null,
                null
        );

        ClassificationResult result = classifier.classify(link);

        assertEquals("unknown", result.category());
    }
}

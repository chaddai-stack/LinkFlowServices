package com.linkflow.api.common.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuggingFaceZeroShotClientTest {

    @Test
    void parseArrayResponseReturnsBestLabel() {
        HuggingFaceZeroShotClient client = new HuggingFaceZeroShotClient(
                new ObjectMapper(),
                false,
                "https://example.com",
                "",
                1000
        );

        var result = client.parseResponse("""
                [
                  {"label": "developer", "score": 0.82},
                  {"label": "shopping", "score": 0.12}
                ]
                """);

        assertTrue(result.isPresent());
        assertEquals("developer", result.get().label());
    }

    @Test
    void parseObjectResponseReturnsBestLabel() {
        HuggingFaceZeroShotClient client = new HuggingFaceZeroShotClient(
                new ObjectMapper(),
                false,
                "https://example.com",
                "",
                1000
        );

        var result = client.parseResponse("""
                {
                  "labels": ["finance", "news"],
                  "scores": [0.77, 0.23]
                }
                """);

        assertTrue(result.isPresent());
        assertEquals("finance", result.get().label());
    }
}

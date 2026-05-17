package com.linkflow.api.common.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuggingFaceZeroShotClient {

    private static final int MAX_INPUT_LENGTH = 1200;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String endpoint;
    private final String apiKey;
    private final int timeoutMillis;

    public HuggingFaceZeroShotClient(
            ObjectMapper objectMapper,
            @Value("${app.ai.huggingface.enabled:false}") boolean enabled,
            @Value("${app.ai.huggingface.zero-shot-endpoint:https://router.huggingface.co/hf-inference/models/facebook/bart-large-mnli}") String endpoint,
            @Value("${app.ai.huggingface.api-key:${HF_TOKEN:}}") String apiKey,
            @Value("${app.ai.huggingface.timeout-ms:3000}") int timeoutMillis
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .build();
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Hugging Face zero-shot 调用封装。
     *
     * 这里故意返回 Optional：远端超时、限流、模型不可用都不影响主流程，
     * 业务层会继续使用本地规则结果。
     */
    public Optional<ZeroShotPrediction> classify(String input, List<String> candidateLabels) {
        if (!enabled || apiKey == null || apiKey.isBlank() || input == null || input.isBlank() || candidateLabels.isEmpty()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = Map.of(
                    "inputs", truncate(input),
                    "parameters", Map.of(
                            "candidate_labels", candidateLabels,
                            "multi_label", false
                    )
            );
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            return parseResponse(response.body());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    Optional<ZeroShotPrediction> parseResponse(String body) {
        try {
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }

            String trimmed = body.trim();
            List<LabelScore> scores;
            if (trimmed.startsWith("[")) {
                scores = parseArrayResponse(trimmed);
            } else {
                scores = parseObjectResponse(trimmed);
            }

            return scores.stream()
                    .max(Comparator.comparingDouble(LabelScore::score))
                    .map(best -> new ZeroShotPrediction(best.label(), best.score(), scores));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private List<LabelScore> parseArrayResponse(String body) throws java.io.IOException {
        List<Map<String, Object>> raw = objectMapper.readValue(body, new TypeReference<>() {
        });
        List<LabelScore> scores = new ArrayList<>();
        for (Map<String, Object> item : raw) {
            Object label = item.get("label");
            Object score = item.get("score");
            if (label != null && score instanceof Number number) {
                scores.add(new LabelScore(String.valueOf(label), number.doubleValue()));
            }
        }
        return scores;
    }

    private List<LabelScore> parseObjectResponse(String body) throws java.io.IOException {
        Map<String, Object> raw = objectMapper.readValue(body, new TypeReference<>() {
        });
        Object labelsValue = raw.get("labels");
        Object scoresValue = raw.get("scores");
        if (!(labelsValue instanceof List<?> labels) || !(scoresValue instanceof List<?> rawScores)) {
            return List.of();
        }

        List<LabelScore> scores = new ArrayList<>();
        int count = Math.min(labels.size(), rawScores.size());
        for (int i = 0; i < count; i++) {
            Object score = rawScores.get(i);
            if (score instanceof Number number) {
                scores.add(new LabelScore(String.valueOf(labels.get(i)), number.doubleValue()));
            }
        }
        return scores;
    }

    private String truncate(String input) {
        return input.length() <= MAX_INPUT_LENGTH ? input : input.substring(0, MAX_INPUT_LENGTH);
    }

    public record ZeroShotPrediction(String label, double score, List<LabelScore> scores) {
    }

    public record LabelScore(String label, double score) {
    }
}

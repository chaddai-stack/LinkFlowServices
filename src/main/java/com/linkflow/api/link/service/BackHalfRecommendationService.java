package com.linkflow.api.link.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.dto.request.BackHalfRecommendationRequest;
import com.linkflow.api.link.dto.response.BackHalfRecommendationResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BackHalfRecommendationService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_BASE_LENGTH = 48;

    private final UrlMappingRepository urlMappingRepository;
    private final LinkTitleCrawlerService linkTitleCrawlerService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean llmEnabled;
    private final String llmEndpoint;
    private final String llmApiKey;
    private final String llmModel;

    public BackHalfRecommendationService(
            UrlMappingRepository urlMappingRepository,
            LinkTitleCrawlerService linkTitleCrawlerService,
            ObjectMapper objectMapper,
            @Value("${app.ai.back-half.enabled:${app.ai.slug.enabled:false}}") boolean llmEnabled,
            @Value("${app.ai.back-half.endpoint:${app.ai.slug.endpoint:https://api.openai.com/v1/chat/completions}}") String llmEndpoint,
            @Value("${app.ai.back-half.api-key:${app.ai.slug.api-key:${OPENAI_API_KEY:}}}") String llmApiKey,
            @Value("${app.ai.back-half.model:${app.ai.slug.model:gpt-4o-mini}}") String llmModel
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.linkTitleCrawlerService = linkTitleCrawlerService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.llmEnabled = llmEnabled;
        this.llmEndpoint = llmEndpoint;
        this.llmApiKey = llmApiKey;
        this.llmModel = llmModel;
    }

    /**
     * 基于本地规则和可选 LLM 推荐可读 back_half。
     */
    public BackHalfRecommendationResponse recommend(BackHalfRecommendationRequest request) {
        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        if ((request.longUrl() == null || request.longUrl().isBlank())
                && (request.title() == null || request.title().isBlank())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of("long_url", "long_url or title must be provided")
            );
        }

        String title = linkTitleCrawlerService.resolveTitle(request.longUrl(), request.title());
        LinkedHashSet<Candidate> candidates = new LinkedHashSet<>();
        localCandidates(request.longUrl(), title)
                .forEach(backHalf -> candidates.add(new Candidate(backHalf, "rules")));
        llmCandidates(request.longUrl(), title)
                .forEach(backHalf -> candidates.add(new Candidate(backHalf, "llm")));

        List<BackHalfRecommendationResponse.BackHalfRecommendationItem> suggestions = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        for (Candidate candidate : candidates) {
            if (suggestions.size() >= limit) {
                break;
            }
            String available = firstAvailable(candidate.backHalf());
            if (emitted.add(available)) {
                suggestions.add(new BackHalfRecommendationResponse.BackHalfRecommendationItem(
                        available,
                        candidate.source(),
                        true
                ));
            }
        }

        return new BackHalfRecommendationResponse(suggestions);
    }

    private List<String> localCandidates(String longUrl, String title) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        addNormalized(result, title);

        if (longUrl != null && !longUrl.isBlank()) {
            URI uri = URI.create(longUrl);
            String host = uri.getHost() == null ? null : uri.getHost().replaceFirst("^www\\.", "");
            if (host != null) {
                addNormalized(result, host.replaceAll("\\.[a-zA-Z]{2,}$", ""));
            }
            String path = uri.getPath();
            if (path != null && !path.isBlank() && !path.equals("/")) {
                String[] parts = path.replaceAll("^/+|/+$", "").split("/");
                if (parts.length > 0) {
                    addNormalized(result, parts[parts.length - 1]);
                }
                if (parts.length > 1) {
                    addNormalized(result, parts[parts.length - 2] + "-" + parts[parts.length - 1]);
                }
            }
        }

        List<String> bases = new ArrayList<>(result);
        for (String base : bases) {
            result.add(base + "-link");
            result.add(base + "-go");
        }
        return new ArrayList<>(result);
    }

    private void addNormalized(Set<String> result, String value) {
        String normalized = normalizeBackHalf(value);
        if (normalized != null) {
            result.add(normalized);
        }
    }

    private List<String> llmCandidates(String longUrl, String title) {
        if (!llmEnabled || llmApiKey == null || llmApiKey.isBlank()) {
            return List.of();
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", llmModel,
                    "temperature", 0.4,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "Return only a JSON array of 8 short URL back-halves. Use lowercase letters, numbers, and hyphens. No prose."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", "URL: " + nullToEmpty(longUrl) + "\nTitle: " + nullToEmpty(title)
                            )
                    )
            );
            HttpRequest request = HttpRequest.newBuilder(URI.create(llmEndpoint))
                    .timeout(Duration.ofSeconds(12))
                    .header("Authorization", "Bearer " + llmApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            Map<String, Object> parsed = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
            if (choices == null || choices.isEmpty()) {
                return List.of();
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                return List.of();
            }
            String content = String.valueOf(message.get("content"));
            List<String> raw = objectMapper.readValue(content, new TypeReference<>() {
            });
            return raw.stream()
                    .map(this::normalizeBackHalf)
                    .filter(value -> value != null)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String firstAvailable(String backHalf) {
        if (!urlMappingRepository.existsByBackHalf(backHalf)) {
            return backHalf;
        }
        for (int i = 2; i <= 99; i++) {
            String next = truncate(backHalf, MAX_BASE_LENGTH - 3) + "-" + i;
            if (!urlMappingRepository.existsByBackHalf(next)) {
                return next;
            }
        }
        return truncate(backHalf, 36) + "-" + Long.toString(System.currentTimeMillis(), 36);
    }

    private String normalizeBackHalf(String value) {
        if (value == null) {
            return null;
        }
        String backHalf = value.toLowerCase(Locale.ROOT)
                .replaceAll("https?://", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
        if (backHalf.length() > MAX_BASE_LENGTH) {
            backHalf = truncate(backHalf, MAX_BASE_LENGTH).replaceAll("-+$", "");
        }
        if (backHalf.length() < 3) {
            return null;
        }
        return backHalf;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Candidate(String backHalf, String source) {
    }
}

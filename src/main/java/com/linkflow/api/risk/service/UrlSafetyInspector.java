package com.linkflow.api.risk.service;

import com.linkflow.api.risk.domain.BlacklistEntryType;
import com.linkflow.api.risk.domain.RiskLevel;
import com.linkflow.api.risk.repository.RiskBlacklistEntryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UrlSafetyInspector {

    private final UrlNormalizationService urlNormalizationService;
    private final RiskBlacklistEntryRepository blacklistRepository;

    public UrlSafetyInspector(
            UrlNormalizationService urlNormalizationService,
            RiskBlacklistEntryRepository blacklistRepository
    ) {
        this.urlNormalizationService = urlNormalizationService;
        this.blacklistRepository = blacklistRepository;
    }

    /**
     * 最小规则检测器。
     *
     * 这层只处理确定性的安全边界：URL 结构、内网地址、黑名单和明显可疑特征。
     * AI/Hugging Face 后续只作为增强 provider，不替代这里的基础防线。
     */
    public RiskInspectionResult inspect(String longUrl) {
        UrlNormalizationService.UrlFacts facts = urlNormalizationService.parse(longUrl);
        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (!facts.valid()) {
            return critical(facts.invalidReason());
        }

        if (!"http".equals(facts.scheme()) && !"https".equals(facts.scheme())) {
            return critical("unsupported_scheme");
        }

        if (facts.privateOrLocalHost()) {
            return critical("private_or_local_target");
        }

        if (blacklistRepository.existsByTypeAndValue(BlacklistEntryType.URL, facts.normalizedUrl())) {
            return critical("blacklisted_url");
        }

        if (blacklistRepository.existsByTypeAndValue(BlacklistEntryType.DOMAIN, facts.host())) {
            return critical("blacklisted_domain");
        }

        if (facts.hasCredentials()) {
            score += 45;
            reasons.add("url_contains_credentials");
        }

        if ("http".equals(facts.scheme())) {
            score += 15;
            reasons.add("plain_http");
        }

        if (facts.ipLiteral()) {
            score += 20;
            reasons.add("ip_literal_host");
        }

        if (facts.normalizedUrl().length() > 512) {
            score += 15;
            reasons.add("very_long_url");
        }

        if (facts.queryParameterCount() > 12) {
            score += 10;
            reasons.add("many_query_parameters");
        }

        if (reasons.isEmpty()) {
            reasons.add("basic_url_checks_passed");
        }

        return new RiskInspectionResult(toLevel(score), Math.min(score, 100), reasons);
    }

    private RiskInspectionResult critical(String reason) {
        return new RiskInspectionResult(RiskLevel.CRITICAL, 100, List.of(reason));
    }

    private RiskLevel toLevel(int score) {
        if (score >= 85) {
            return RiskLevel.CRITICAL;
        }
        if (score >= 60) {
            return RiskLevel.HIGH;
        }
        if (score >= 30) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}

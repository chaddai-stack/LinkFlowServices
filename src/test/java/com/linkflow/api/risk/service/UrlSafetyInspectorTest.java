package com.linkflow.api.risk.service;

import com.linkflow.api.risk.domain.BlacklistEntryType;
import com.linkflow.api.risk.domain.RiskLevel;
import com.linkflow.api.risk.repository.RiskBlacklistEntryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlSafetyInspectorTest {

    @Test
    void inspectAllowsNormalHttpsUrlWithLowRisk() {
        RiskBlacklistEntryRepository repository = Mockito.mock(RiskBlacklistEntryRepository.class);
        UrlSafetyInspector inspector = new UrlSafetyInspector(new UrlNormalizationService(), repository);

        RiskInspectionResult result = inspector.inspect("https://github.com/example/repo");

        assertEquals(RiskLevel.LOW, result.riskLevel());
        assertEquals(0, result.riskScore());
        assertTrue(result.reasons().contains("basic_url_checks_passed"));
    }

    @Test
    void inspectBlocksLocalNetworkTarget() {
        RiskBlacklistEntryRepository repository = Mockito.mock(RiskBlacklistEntryRepository.class);
        UrlSafetyInspector inspector = new UrlSafetyInspector(new UrlNormalizationService(), repository);

        RiskInspectionResult result = inspector.inspect("http://127.0.0.1/admin");

        assertEquals(RiskLevel.CRITICAL, result.riskLevel());
        assertEquals(100, result.riskScore());
        assertTrue(result.reasons().contains("private_or_local_target"));
    }

    @Test
    void inspectBlocksBlacklistedDomain() {
        RiskBlacklistEntryRepository repository = Mockito.mock(RiskBlacklistEntryRepository.class);
        Mockito.when(repository.existsByTypeAndValue(BlacklistEntryType.DOMAIN, "bad.example"))
                .thenReturn(true);
        UrlSafetyInspector inspector = new UrlSafetyInspector(new UrlNormalizationService(), repository);

        RiskInspectionResult result = inspector.inspect("https://bad.example/login");

        assertEquals(RiskLevel.CRITICAL, result.riskLevel());
        assertTrue(result.reasons().contains("blacklisted_domain"));
    }
}

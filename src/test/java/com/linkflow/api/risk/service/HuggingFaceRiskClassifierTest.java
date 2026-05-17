package com.linkflow.api.risk.service;

import com.linkflow.api.common.ai.HuggingFaceZeroShotClient;
import com.linkflow.api.risk.domain.RiskLevel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class HuggingFaceRiskClassifierTest {

    @Test
    void classifyMapsPhishingToCriticalRisk() {
        HuggingFaceZeroShotClient client = Mockito.mock(HuggingFaceZeroShotClient.class);
        when(client.classify(anyString(), anyList())).thenReturn(Optional.of(
                new HuggingFaceZeroShotClient.ZeroShotPrediction(
                        "phishing",
                        0.92,
                        List.of()
                )
        ));
        HuggingFaceRiskClassifier classifier = new HuggingFaceRiskClassifier(client);

        Optional<RiskInspectionResult> result = classifier.classify("https://example.com/login");

        assertTrue(result.isPresent());
        assertEquals(RiskLevel.CRITICAL, result.get().riskLevel());
        assertTrue(result.get().reasons().contains("hf_phishing"));
    }
}

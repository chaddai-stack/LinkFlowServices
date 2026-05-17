package com.linkflow.api.link.service;

import com.linkflow.api.common.ai.HuggingFaceZeroShotClient;
import com.linkflow.api.link.domain.UrlMapping;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class HuggingFaceLinkClassifierTest {

    @Test
    void classifyMapsZeroShotPredictionToClassificationResult() {
        HuggingFaceZeroShotClient client = Mockito.mock(HuggingFaceZeroShotClient.class);
        when(client.classify(anyString(), anyList())).thenReturn(Optional.of(
                new HuggingFaceZeroShotClient.ZeroShotPrediction(
                        "developer",
                        0.88,
                        List.of()
                )
        ));
        HuggingFaceLinkClassifier classifier = new HuggingFaceLinkClassifier(client);
        UrlMapping link = new UrlMapping("https://github.com/example/repo", "repo", "Example repository", "docs", null);

        Optional<ClassificationResult> result = classifier.classify(link);

        assertTrue(result.isPresent());
        assertEquals("developer", result.get().category());
        assertEquals("huggingface", result.get().source());
    }
}

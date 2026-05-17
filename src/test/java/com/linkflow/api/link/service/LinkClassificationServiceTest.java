package com.linkflow.api.link.service;

import com.linkflow.api.link.domain.LinkClassification;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.repository.LinkClassificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LinkClassificationServiceTest {

    @Test
    void classifyPersistsRuleBasedResult() {
        UUID linkId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        LinkQueryService queryService = Mockito.mock(LinkQueryService.class);
        LinkClassificationRepository repository = Mockito.mock(LinkClassificationRepository.class);
        LinkClassificationService service = new LinkClassificationService(
                queryService,
                repository,
                new RuleBasedLinkClassifier(),
                huggingFaceClassifier(Optional.empty())
        );
        UrlMapping link = Mockito.mock(UrlMapping.class);
        when(link.getPublicId()).thenReturn(linkId);
        when(link.getLongUrl()).thenReturn("https://github.com/example/repo");
        when(link.getTitle()).thenReturn("Example repository");
        when(link.getChannel()).thenReturn("docs");
        when(queryService.getMappingById(linkId)).thenReturn(link);
        when(repository.findByLink(link)).thenReturn(Optional.empty());
        when(repository.save(any(LinkClassification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.classify(linkId);

        assertEquals(linkId, response.linkId());
        assertEquals("developer", response.category());
        assertEquals("rules", response.source());
    }

    @Test
    void classifyUsesHuggingFaceWhenItHasStrongSignal() {
        UUID linkId = UUID.fromString("00000000-0000-0000-0000-000000000008");
        LinkQueryService queryService = Mockito.mock(LinkQueryService.class);
        LinkClassificationRepository repository = Mockito.mock(LinkClassificationRepository.class);
        LinkClassificationService service = new LinkClassificationService(
                queryService,
                repository,
                new RuleBasedLinkClassifier(),
                huggingFaceClassifier(Optional.of(new ClassificationResult(
                        "finance",
                        0.91,
                        java.util.List.of("hf_zero_shot", "hf_label_finance"),
                        "huggingface"
                )))
        );
        UrlMapping link = Mockito.mock(UrlMapping.class);
        when(link.getPublicId()).thenReturn(linkId);
        when(link.getLongUrl()).thenReturn("https://example.com/page");
        when(link.getTitle()).thenReturn("Plain page");
        when(queryService.getMappingById(linkId)).thenReturn(link);
        when(repository.findByLink(link)).thenReturn(Optional.empty());
        when(repository.save(any(LinkClassification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.classify(linkId);

        assertEquals("finance", response.category());
        assertEquals("rules+huggingface", response.source());
    }

    private HuggingFaceLinkClassifier huggingFaceClassifier(Optional<ClassificationResult> result) {
        HuggingFaceLinkClassifier classifier = Mockito.mock(HuggingFaceLinkClassifier.class);
        when(classifier.classify(any(UrlMapping.class))).thenReturn(result);
        return classifier;
    }
}

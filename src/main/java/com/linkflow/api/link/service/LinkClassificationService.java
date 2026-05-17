package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.domain.LinkClassification;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.response.LinkClassificationResponse;
import com.linkflow.api.link.repository.LinkClassificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LinkClassificationService {

    private final LinkQueryService linkQueryService;
    private final LinkClassificationRepository classificationRepository;
    private final RuleBasedLinkClassifier ruleBasedLinkClassifier;
    private final HuggingFaceLinkClassifier huggingFaceLinkClassifier;

    public LinkClassificationService(
            LinkQueryService linkQueryService,
            LinkClassificationRepository classificationRepository,
            RuleBasedLinkClassifier ruleBasedLinkClassifier,
            HuggingFaceLinkClassifier huggingFaceLinkClassifier
    ) {
        this.linkQueryService = linkQueryService;
        this.classificationRepository = classificationRepository;
        this.ruleBasedLinkClassifier = ruleBasedLinkClassifier;
        this.huggingFaceLinkClassifier = huggingFaceLinkClassifier;
    }

    /**
     * 为单个链接生成或刷新分类。
     *
     * 规则分类是稳定底座；Hugging Face 是可选增强层。
     * 当 HF 不可用、超时或置信度过低时，仍返回规则结果。
     */
    @Transactional
    public LinkClassificationResponse classify(UUID linkId) {
        UrlMapping link = linkQueryService.getMappingById(linkId);
        ClassificationResult result = merge(
                ruleBasedLinkClassifier.classify(link),
                huggingFaceLinkClassifier.classify(link)
        );
        String labels = join(result.labels());

        LinkClassification classification = classificationRepository.findByLink(link)
                .map(existing -> {
                    existing.update(result.category(), result.confidence(), labels, result.source());
                    return existing;
                })
                .orElseGet(() -> classificationRepository.save(new LinkClassification(
                        link,
                        result.category(),
                        result.confidence(),
                        labels,
                        result.source()
                )));

        return LinkClassificationResponse.from(classification);
    }

    @Transactional(readOnly = true)
    public LinkClassificationResponse get(UUID linkId) {
        UrlMapping link = linkQueryService.getMappingById(linkId);
        return classificationRepository.findByLink(link)
                .map(LinkClassificationResponse::from)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "LINK_CLASSIFICATION_NOT_FOUND",
                        "Link classification does not exist.",
                        Map.of("link_id", linkId)
                ));
    }

    public static String join(List<String> values) {
        return String.join(",", values);
    }

    public static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private ClassificationResult merge(ClassificationResult rules, Optional<ClassificationResult> huggingFace) {
        if (huggingFace.isEmpty()) {
            return rules;
        }

        ClassificationResult hf = huggingFace.get();
        LinkedHashSet<String> labels = new LinkedHashSet<>(rules.labels());
        labels.addAll(hf.labels());

        boolean preferHuggingFace = "unknown".equals(rules.category())
                || hf.confidence() >= 0.65
                || hf.confidence() >= rules.confidence() + 0.08;

        if (preferHuggingFace) {
            return new ClassificationResult(
                    hf.category(),
                    hf.confidence(),
                    List.copyOf(labels),
                    "rules+huggingface"
            );
        }

        return new ClassificationResult(
                rules.category(),
                rules.confidence(),
                List.copyOf(labels),
                "rules+huggingface"
        );
    }
}

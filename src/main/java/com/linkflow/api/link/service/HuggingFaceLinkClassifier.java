package com.linkflow.api.link.service;

import com.linkflow.api.common.ai.HuggingFaceZeroShotClient;
import com.linkflow.api.link.domain.UrlMapping;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HuggingFaceLinkClassifier {

    private static final List<String> CANDIDATE_LABELS = List.of(
            "shopping",
            "news",
            "social",
            "developer",
            "finance",
            "unknown"
    );
    private static final double MIN_CONFIDENCE = 0.45;

    private final HuggingFaceZeroShotClient zeroShotClient;

    public HuggingFaceLinkClassifier(HuggingFaceZeroShotClient zeroShotClient) {
        this.zeroShotClient = zeroShotClient;
    }

    /**
     * Hugging Face 分类增强。
     *
     * 只根据 URL、标题、渠道构造短文本，不抓取页面内容，避免分类接口引入网络爬取风险。
     */
    public Optional<ClassificationResult> classify(UrlMapping link) {
        String input = "URL: " + safe(link.getLongUrl())
                + "\nTitle: " + safe(link.getTitle())
                + "\nChannel: " + safe(link.getChannel());

        return zeroShotClient.classify(input, CANDIDATE_LABELS)
                .filter(prediction -> prediction.score() >= MIN_CONFIDENCE)
                .map(prediction -> new ClassificationResult(
                        prediction.label(),
                        prediction.score(),
                        List.of("hf_zero_shot", "hf_label_" + prediction.label()),
                        "huggingface"
                ));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

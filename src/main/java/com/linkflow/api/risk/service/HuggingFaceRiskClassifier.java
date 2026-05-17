package com.linkflow.api.risk.service;

import com.linkflow.api.common.ai.HuggingFaceZeroShotClient;
import com.linkflow.api.risk.domain.RiskLevel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HuggingFaceRiskClassifier {

    private static final List<String> CANDIDATE_LABELS = List.of(
            "safe",
            "phishing",
            "malware",
            "spam",
            "adult",
            "gambling"
    );
    private static final double MIN_CONFIDENCE = 0.55;

    private final HuggingFaceZeroShotClient zeroShotClient;

    public HuggingFaceRiskClassifier(HuggingFaceZeroShotClient zeroShotClient) {
        this.zeroShotClient = zeroShotClient;
    }

    /**
     * Hugging Face 风险语义增强。
     *
     * 这不是最终安全边界，只给规则检测结果增加语义信号；
     * 内网、黑名单等硬规则仍由 UrlSafetyInspector 决定。
     */
    public Optional<RiskInspectionResult> classify(String longUrl) {
        String input = "Classify the safety risk of this URL for a public short-link service: " + longUrl;
        return zeroShotClient.classify(input, CANDIDATE_LABELS)
                .filter(prediction -> prediction.score() >= MIN_CONFIDENCE)
                .map(prediction -> toRiskResult(prediction.label(), prediction.score()));
    }

    private RiskInspectionResult toRiskResult(String label, double confidence) {
        int score = (int) Math.round(confidence * 100);
        return switch (label) {
            case "phishing", "malware" -> new RiskInspectionResult(
                    RiskLevel.CRITICAL,
                    Math.max(score, 90),
                    List.of("hf_" + label)
            );
            case "spam", "adult", "gambling" -> new RiskInspectionResult(
                    RiskLevel.MEDIUM,
                    Math.max(score, 45),
                    List.of("hf_" + label)
            );
            case "safe" -> new RiskInspectionResult(
                    RiskLevel.LOW,
                    Math.min(score, 10),
                    List.of("hf_safe")
            );
            default -> new RiskInspectionResult(
                    RiskLevel.UNKNOWN,
                    0,
                    List.of("hf_unknown")
            );
        };
    }
}

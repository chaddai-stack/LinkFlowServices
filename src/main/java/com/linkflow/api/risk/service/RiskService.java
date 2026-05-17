package com.linkflow.api.risk.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.domain.LinkStatus;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.service.LinkQueryService;
import com.linkflow.api.risk.domain.BlacklistEntryType;
import com.linkflow.api.risk.domain.RiskAlert;
import com.linkflow.api.risk.domain.RiskAlertStatus;
import com.linkflow.api.risk.domain.RiskBlacklistEntry;
import com.linkflow.api.risk.domain.RiskLevel;
import com.linkflow.api.risk.domain.RiskScanTask;
import com.linkflow.api.risk.dto.BlacklistRequest;
import com.linkflow.api.risk.dto.RiskAlertResponse;
import com.linkflow.api.risk.dto.RiskScanTaskRequest;
import com.linkflow.api.risk.dto.RiskScanTaskResponse;
import com.linkflow.api.risk.repository.RiskAlertRepository;
import com.linkflow.api.risk.repository.RiskBlacklistEntryRepository;
import com.linkflow.api.risk.repository.RiskScanTaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class RiskService {

    private final LinkQueryService linkQueryService;
    private final RiskScanTaskRepository scanTaskRepository;
    private final RiskAlertRepository alertRepository;
    private final RiskBlacklistEntryRepository blacklistRepository;
    private final UrlNormalizationService urlNormalizationService;
    private final UrlSafetyInspector urlSafetyInspector;
    private final HuggingFaceRiskClassifier huggingFaceRiskClassifier;
    private final String publicBaseUrl;

    public RiskService(
            LinkQueryService linkQueryService,
            RiskScanTaskRepository scanTaskRepository,
            RiskAlertRepository alertRepository,
            RiskBlacklistEntryRepository blacklistRepository,
            UrlNormalizationService urlNormalizationService,
            UrlSafetyInspector urlSafetyInspector,
            HuggingFaceRiskClassifier huggingFaceRiskClassifier,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.linkQueryService = linkQueryService;
        this.scanTaskRepository = scanTaskRepository;
        this.alertRepository = alertRepository;
        this.blacklistRepository = blacklistRepository;
        this.urlNormalizationService = urlNormalizationService;
        this.urlSafetyInspector = urlSafetyInspector;
        this.huggingFaceRiskClassifier = huggingFaceRiskClassifier;
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * 查询风险告警列表。
     * 告警来自检测任务的高风险结果，先落库保证前端可查询、可审核。
     */
    @Transactional(readOnly = true)
    public List<RiskAlertResponse> listAlerts(String riskLevel, String status, String search) {
        Specification<RiskAlert> spec = alertSpecification(riskLevel, status, search);
        return alertRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(alert -> RiskAlertResponse.from(alert, publicBaseUrl))
                .toList();
    }

    /**
     * 查询单个风险告警。
     */
    @Transactional(readOnly = true)
    public RiskAlertResponse getAlert(UUID alertId) {
        return RiskAlertResponse.from(getAlertEntity(alertId), publicBaseUrl);
    }

    /**
     * 创建并立即执行最小检测任务。
     * 目前是同步闭环：任务落库 -> 规则检测 -> HF 语义增强 -> 写回结果 -> 必要时生成告警。
     * 后续接 RabbitMQ 时，可以把执行部分替换成异步 worker。
     */
    @Transactional
    public RiskScanTaskResponse createScanTask(RiskScanTaskRequest request) {
        if (request.linkId() == null && (request.longUrl() == null || request.longUrl().isBlank())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of("link_id", "link_id or long_url must be provided")
            );
        }

        UrlMapping link = null;
        String longUrl = request.longUrl();
        if (request.linkId() != null) {
            link = linkQueryService.getMappingById(request.linkId());
            longUrl = link.getLongUrl();
        }

        RiskScanTask task = scanTaskRepository.save(new RiskScanTask(link, longUrl));
        task.markRunning();

        try {
            RiskInspectionResult result = enhanceRisk(urlSafetyInspector.inspect(longUrl), longUrl);
            task.markSucceeded(result.riskLevel(), result.riskScore(), RiskResponseMapper.joinCodes(result.reasons()));
            createAlertIfNeeded(task, result);
        } catch (RuntimeException ex) {
            task.markFailed(ex.getMessage() == null ? "scan failed" : ex.getMessage());
        }

        return RiskScanTaskResponse.from(task);
    }

    /**
     * 查询检测任务。
     */
    @Transactional(readOnly = true)
    public RiskScanTaskResponse getScanTask(UUID taskId) {
        return RiskScanTaskResponse.from(getTaskEntity(taskId));
    }

    /**
     * 审核告警。
     * blocked/blacklisted 会同步把对应短链状态置为 BLOCKED，避免已确认风险链接继续跳转。
     */
    @Transactional
    public Map<String, Object> reviewAlert(UUID alertId, String action, String comment) {
        RiskAlert alert = getAlertEntity(alertId);
        RiskAlertStatus nextStatus = RiskAlertStatus.fromWireValue(action);
        alert.review(nextStatus);

        if ((nextStatus == RiskAlertStatus.BLOCKED || nextStatus == RiskAlertStatus.BLACKLISTED)
                && alert.getLink() != null) {
            alert.getLink().updateStatus(LinkStatus.BLOCKED);
        }

        return Map.of("alert_id", alertId, "action", nextStatus.wireValue());
    }

    /**
     * 添加域名黑名单。
     */
    @Transactional
    public Map<String, Object> addDomainBlacklist(BlacklistRequest request) {
        String value = urlNormalizationService.normalizeDomain(request.value());
        upsertBlacklist(BlacklistEntryType.DOMAIN, value, request.reason());
        return Map.of("value", value, "type", "domain", "status", "active");
    }

    /**
     * 添加 URL 黑名单。
     */
    @Transactional
    public Map<String, Object> addUrlBlacklist(BlacklistRequest request) {
        String value = urlNormalizationService.normalizeUrl(request.value());
        upsertBlacklist(BlacklistEntryType.URL, value, request.reason());
        return Map.of("value", value, "type", "url", "status", "active");
    }

    private RiskInspectionResult enhanceRisk(RiskInspectionResult rules, String longUrl) {
        if (rules.riskLevel() == RiskLevel.CRITICAL) {
            return rules;
        }

        return huggingFaceRiskClassifier.classify(longUrl)
                .map(hf -> mergeRisk(rules, hf))
                .orElse(rules);
    }

    private RiskInspectionResult mergeRisk(RiskInspectionResult rules, RiskInspectionResult huggingFace) {
        List<String> reasons = new ArrayList<>(rules.reasons());
        reasons.addAll(huggingFace.reasons());

        return new RiskInspectionResult(
                higherRiskLevel(rules.riskLevel(), huggingFace.riskLevel()),
                Math.max(rules.riskScore(), huggingFace.riskScore()),
                reasons
        );
    }

    private RiskLevel higherRiskLevel(RiskLevel left, RiskLevel right) {
        return severity(right) > severity(left) ? right : left;
    }

    private int severity(RiskLevel level) {
        return switch (level) {
            case UNKNOWN -> 0;
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }

    private void createAlertIfNeeded(RiskScanTask task, RiskInspectionResult result) {
        if (task.getLink() == null) {
            return;
        }

        if (result.riskLevel() != RiskLevel.HIGH && result.riskLevel() != RiskLevel.CRITICAL) {
            return;
        }

        String title = task.getLink().getTitle() == null || task.getLink().getTitle().isBlank()
                ? "Risk detected for " + task.getLink().getBackHalf()
                : task.getLink().getTitle();
        alertRepository.save(new RiskAlert(
                task.getLink(),
                result.riskLevel(),
                result.riskScore(),
                title,
                RiskResponseMapper.joinCodes(result.reasons())
        ));
    }

    private void upsertBlacklist(BlacklistEntryType type, String value, String reason) {
        if (value == null || value.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of("value", "must be a valid " + type.wireValue())
            );
        }

        blacklistRepository.findByTypeAndValue(type, value)
                .ifPresentOrElse(
                        entry -> entry.updateReason(reason),
                        () -> saveBlacklist(type, value, reason)
                );
    }

    private void saveBlacklist(BlacklistEntryType type, String value, String reason) {
        try {
            blacklistRepository.save(new RiskBlacklistEntry(type, value, reason));
        } catch (DataIntegrityViolationException ignored) {
            blacklistRepository.findByTypeAndValue(type, value)
                    .ifPresent(entry -> entry.updateReason(reason));
        }
    }

    private RiskScanTask getTaskEntity(UUID taskId) {
        return scanTaskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "RISK_SCAN_TASK_NOT_FOUND",
                        "Risk scan task does not exist.",
                        Map.of("task_id", taskId)
                ));
    }

    private RiskAlert getAlertEntity(UUID alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "RISK_ALERT_NOT_FOUND",
                        "Risk alert does not exist.",
                        Map.of("alert_id", alertId)
                ));
    }

    private Specification<RiskAlert> alertSpecification(String riskLevel, String status, String search) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            if (riskLevel != null && !riskLevel.isBlank()) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(
                        root.get("riskLevel"),
                        RiskLevel.fromWireValue(riskLevel)
                ));
            }

            if (status != null && !status.isBlank()) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(
                        root.get("status"),
                        RiskAlertStatus.fromWireValue(status)
                ));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern));
            }

            return predicate;
        };
    }
}

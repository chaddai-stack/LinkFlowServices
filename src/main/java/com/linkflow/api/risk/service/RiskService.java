package com.linkflow.api.risk.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.service.LinkQueryService;
import com.linkflow.api.risk.dto.BlacklistRequest;
import com.linkflow.api.risk.dto.RiskAlertResponse;
import com.linkflow.api.risk.dto.RiskScanTaskRequest;
import com.linkflow.api.risk.dto.RiskScanTaskResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RiskService {

    private final LinkQueryService linkQueryService;
    private final Map<UUID, RiskScanTaskResponse> scanTasks = new ConcurrentHashMap<>();
    private final Map<String, BlacklistRequest> domainBlacklist = new ConcurrentHashMap<>();
    private final Map<String, BlacklistRequest> urlBlacklist = new ConcurrentHashMap<>();

    public RiskService(LinkQueryService linkQueryService) {
        this.linkQueryService = linkQueryService;
    }

    /**
     * 查询风险告警列表
     *
     * 当前没有 alert 持久化模型，返回空集合；后续接入风控扫描结果表后替换这里。
     */
    public List<RiskAlertResponse> listAlerts(String riskLevel, String status, String search) {
        return List.of();
    }

    /**
     * 查询单个风险告警
     */
    public RiskAlertResponse getAlert(UUID alertId) {
        throw new ApiException(
                HttpStatus.NOT_FOUND,
                "RISK_ALERT_NOT_FOUND",
                "Risk alert does not exist.",
                Map.of("alert_id", alertId)
        );
    }

    /**
     * 创建扫描任务
     *
     * linkId 和 longUrl 至少提供一个；如果提供 linkId，则以数据库中对应短链的 longUrl 为准。
     */
    public RiskScanTaskResponse createScanTask(RiskScanTaskRequest request) {
        if (request.linkId() == null && (request.longUrl() == null || request.longUrl().isBlank())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of("link_id", "link_id or long_url must be provided")
            );
        }

        String longUrl = request.longUrl();
        if (request.linkId() != null) {
            longUrl = linkQueryService.getMappingById(request.linkId()).getLongUrl();
        }

        RiskScanTaskResponse task = new RiskScanTaskResponse(
                UUID.randomUUID(),
                request.linkId(),
                longUrl,
                "queued",
                OffsetDateTime.now()
        );
        scanTasks.put(task.taskId(), task);
        return task;
    }

    /**
     * 查询内存扫描任务
     */
    public RiskScanTaskResponse getScanTask(UUID taskId) {
        RiskScanTaskResponse task = scanTasks.get(taskId);
        if (task == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "RISK_SCAN_TASK_NOT_FOUND",
                    "Risk scan task does not exist.",
                    Map.of("task_id", taskId)
            );
        }
        return task;
    }

    /**
     * 审核告警
     */
    public Map<String, Object> reviewAlert(UUID alertId, String action, String comment) {
        getAlert(alertId);
        return Map.of("alert_id", alertId, "action", action);
    }

    /**
     * 添加域名黑名单
     */
    public Map<String, Object> addDomainBlacklist(BlacklistRequest request) {
        domainBlacklist.put(request.value(), request);
        return Map.of("value", request.value(), "type", "domain", "status", "active");
    }

    /**
     * 添加 URL 黑名单
     */
    public Map<String, Object> addUrlBlacklist(BlacklistRequest request) {
        urlBlacklist.put(request.value(), request);
        return Map.of("value", request.value(), "type", "url", "status", "active");
    }
}

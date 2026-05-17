package com.linkflow.api.risk.domain;

import com.linkflow.api.link.domain.UrlMapping;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_scan_tasks")
public class RiskScanTask {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id")
    private UrlMapping link;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RiskScanStatus status = RiskScanStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel = RiskLevel.UNKNOWN;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "reason_codes", nullable = false, columnDefinition = "TEXT")
    private String reasonCodes = "";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    protected RiskScanTask() {
    }

    public RiskScanTask(UrlMapping link, String longUrl) {
        this.link = link;
        this.longUrl = longUrl;
    }

    public UUID getId() {
        return id;
    }

    public UrlMapping getLink() {
        return link;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public RiskScanStatus getStatus() {
        return status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getReasonCodes() {
        return reasonCodes;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void markRunning() {
        this.status = RiskScanStatus.RUNNING;
    }

    public void markSucceeded(RiskLevel riskLevel, int riskScore, String reasonCodes) {
        this.status = RiskScanStatus.SUCCEEDED;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.reasonCodes = reasonCodes;
        this.finishedAt = OffsetDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = RiskScanStatus.FAILED;
        this.riskLevel = RiskLevel.UNKNOWN;
        this.riskScore = 0;
        this.errorMessage = errorMessage;
        this.finishedAt = OffsetDateTime.now();
    }

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

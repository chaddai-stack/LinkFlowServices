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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_alerts")
public class RiskAlert {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id")
    private UrlMapping link;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "reason_codes", nullable = false, columnDefinition = "TEXT")
    private String reasonCodes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RiskAlertStatus status = RiskAlertStatus.PENDING;

    @Column(name = "reporter", nullable = false, length = 120)
    private String reporter = "rule-risk-engine";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected RiskAlert() {
    }

    public RiskAlert(UrlMapping link, RiskLevel riskLevel, int riskScore, String title, String reasonCodes) {
        this.link = link;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.title = title;
        this.reasonCodes = reasonCodes;
    }

    public UUID getId() {
        return id;
    }

    public UrlMapping getLink() {
        return link;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getTitle() {
        return title;
    }

    public String getReasonCodes() {
        return reasonCodes;
    }

    public RiskAlertStatus getStatus() {
        return status;
    }

    public String getReporter() {
        return reporter;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void review(RiskAlertStatus status) {
        this.status = status;
    }

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

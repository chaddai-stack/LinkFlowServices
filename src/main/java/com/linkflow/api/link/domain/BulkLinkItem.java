package com.linkflow.api.link.domain;

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
@Table(name = "bulk_link_job_items")
public class BulkLinkItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private BulkLinkJob job;

    @Column(name = "item_index", nullable = false)
    private int itemIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BulkLinkItemStatus status = BulkLinkItemStatus.PENDING;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;

    @Column(name = "title", length = 300)
    private String title;

    @Column(name = "custom_back_half", length = 80)
    private String customBackHalf;

    @Column(name = "channel", length = 80)
    private String channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id")
    private UrlMapping link;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "category", length = 40)
    private String category;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected BulkLinkItem() {
    }

    public BulkLinkItem(BulkLinkJob job, int itemIndex, String longUrl, String title, String customBackHalf, String channel) {
        this.job = job;
        this.itemIndex = itemIndex;
        this.longUrl = longUrl;
        this.title = title;
        this.customBackHalf = customBackHalf;
        this.channel = channel;
    }

    public UUID getId() {
        return id;
    }

    public BulkLinkJob getJob() {
        return job;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    public BulkLinkItemStatus getStatus() {
        return status;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getCustomBackHalf() {
        return customBackHalf;
    }

    public String getChannel() {
        return channel;
    }

    public UrlMapping getLink() {
        return link;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public String getCategory() {
        return category;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void markSucceeded(UrlMapping link, String riskLevel, Integer riskScore, String category) {
        this.status = BulkLinkItemStatus.SUCCEEDED;
        this.link = link;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.category = category;
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void markFailed(String errorCode, String errorMessage) {
        this.status = BulkLinkItemStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage == null ? null : errorMessage.substring(0, Math.min(errorMessage.length(), 1000));
    }

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

package com.linkflow.api.link.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_link_jobs")
public class BulkLinkJob {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BulkLinkJobStatus status = BulkLinkJobStatus.RUNNING;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "succeeded_count", nullable = false)
    private int succeededCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected BulkLinkJob() {
    }

    public BulkLinkJob(int totalCount) {
        this.totalCount = totalCount;
    }

    public UUID getId() {
        return id;
    }

    public BulkLinkJobStatus getStatus() {
        return status;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getSucceededCount() {
        return succeededCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 根据逐条处理结果收口 job 状态，避免前端需要自行推断。
     */
    public void complete(int succeededCount, int failedCount) {
        this.succeededCount = succeededCount;
        this.failedCount = failedCount;
        if (succeededCount == totalCount) {
            this.status = BulkLinkJobStatus.SUCCEEDED;
        } else if (failedCount == totalCount) {
            this.status = BulkLinkJobStatus.FAILED;
        } else {
            this.status = BulkLinkJobStatus.PARTIAL_FAILED;
        }
    }

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

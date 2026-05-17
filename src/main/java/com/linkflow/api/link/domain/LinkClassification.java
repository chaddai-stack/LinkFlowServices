package com.linkflow.api.link.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "link_classifications")
public class LinkClassification {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id", nullable = false, unique = true)
    private UrlMapping link;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "labels", nullable = false, columnDefinition = "TEXT")
    private String labels;

    @Column(name = "source", nullable = false, length = 40)
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected LinkClassification() {
    }

    public LinkClassification(UrlMapping link, String category, double confidence, String labels, String source) {
        this.link = link;
        this.category = category;
        this.confidence = confidence;
        this.labels = labels;
        this.source = source;
    }

    public UUID getId() {
        return id;
    }

    public UrlMapping getLink() {
        return link;
    }

    public String getCategory() {
        return category;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getLabels() {
        return labels;
    }

    public String getSource() {
        return source;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String category, double confidence, String labels, String source) {
        this.category = category;
        this.confidence = confidence;
        this.labels = labels;
        this.source = source;
    }

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

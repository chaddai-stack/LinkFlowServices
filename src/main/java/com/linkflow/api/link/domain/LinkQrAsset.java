package com.linkflow.api.link.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "link_qr_assets")
public class LinkQrAsset {

    /*
     * 二维码资产 自身的公开唯一标识
     */
    @Id
    private UUID id;

    /*
     * 内部 link 主键
     *
     * 资产表使用内部 内部 Long id 做外键；响应 DTO 再映射回 UrlMapping.公开 ID 给前端。
     */
    @Column(name = "link_id", nullable = false)
    private Long linkId;

    @Column(name = "format", nullable = false, length = 20)
    private String format;

    @Column(name = "size", nullable = false)
    private int size;

    @Column(name = "storage_key", nullable = false, columnDefinition = "TEXT")
    private String storageKey;

    @Column(name = "storage_url", nullable = false, columnDefinition = "TEXT")
    private String storageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LinkQrAsset() {
    }

    public LinkQrAsset(Long linkId, String format, int size, String storageKey, String storageUrl) {
        this.linkId = linkId;
        this.format = format;
        this.size = size;
        this.storageKey = storageKey;
        this.storageUrl = storageUrl;
    }

    public UUID getId() {
        return id;
    }

    public Long getLinkId() {
        return linkId;
    }

    public String getFormat() {
        return format;
    }

    public int getSize() {
        return size;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 持久化前分配 asset UUID
     */
    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

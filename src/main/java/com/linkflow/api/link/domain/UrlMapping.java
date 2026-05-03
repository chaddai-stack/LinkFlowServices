package com.linkflow.api.link.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "url_mapping")
public class UrlMapping {

    /*
     * 内部主键
     *
     * 对数据库关系和外键友好，但不直接暴露给外部 API，避免自增 id 被枚举。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 对外公开 ID
     *
     * 公开 ID 使用随机 UUID，与内部 内部 Long id 解耦；所有新版 Link API 都通过它寻址。
     */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;

    @Column(name = "back_half", nullable = false, unique = true, length = 80)
    private String backHalf;

    @Column(name = "title", length = 300)
    private String title;

    @Column(name = "channel", length = 80)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LinkStatus status = LinkStatus.ACTIVE;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected UrlMapping() {
    }

    public UrlMapping(String longUrl, String backHalf) {
        this.backHalf = backHalf;
        this.longUrl = longUrl;
    }

    public UrlMapping(String longUrl, String backHalf, String title, String channel, OffsetDateTime expiresAt) {
        this.backHalf = backHalf;
        this.longUrl = longUrl;
        this.title = title;
        this.channel = channel;
        this.expiresAt = expiresAt;
        this.status = LinkStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public String getBackHalf() {
        return backHalf;
    }

    public String getTitle() {
        return title;
    }

    public String getChannel() {
        return channel;
    }

    public LinkStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(String longUrl, String backHalf, String title, String channel, OffsetDateTime expiresAt) {
        this.longUrl = longUrl;
        this.backHalf = backHalf;
        this.title = title;
        this.channel = channel;
        this.expiresAt = expiresAt;
    }

    public void updateStatus(LinkStatus status) {
        this.status = status;
    }

    /**
     * 持久化前分配 公开 UUID
     */
    @PrePersist
    void assignPublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}

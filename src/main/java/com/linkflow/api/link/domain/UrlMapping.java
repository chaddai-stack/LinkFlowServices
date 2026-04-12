package com.linkflow.api.link.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "url_mapping")
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;

    @Column(name = "slug", nullable = false, unique = true, length = 80)
    private String slug;

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

    public UrlMapping(String longUrl, String slug) {
        this.slug = slug;
        this.longUrl = longUrl;
    }

    public UrlMapping(String longUrl, String slug, String title, String channel, OffsetDateTime expiresAt) {
        this.slug = slug;
        this.longUrl = longUrl;
        this.title = title;
        this.channel = channel;
        this.expiresAt = expiresAt;
        this.status = LinkStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public String getSlug() {
        return slug;
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

    public void updateDetails(String longUrl, String slug, String title, String channel, OffsetDateTime expiresAt) {
        this.longUrl = longUrl;
        this.slug = slug;
        this.title = title;
        this.channel = channel;
        this.expiresAt = expiresAt;
    }

    public void updateStatus(LinkStatus status) {
        this.status = status;
    }
}

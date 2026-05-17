package com.linkflow.api.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "risk_blacklist_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_risk_blacklist_type_value", columnNames = {"type", "value"})
)
public class RiskBlacklistEntry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private BlacklistEntryType type;

    @Column(name = "value", nullable = false, length = 2048)
    private String value;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "source", nullable = false, length = 40)
    private String source = "manual";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected RiskBlacklistEntry() {
    }

    public RiskBlacklistEntry(BlacklistEntryType type, String value, String reason) {
        this.type = type;
        this.value = value;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public BlacklistEntryType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getReason() {
        return reason;
    }

    public String getSource() {
        return source;
    }

    public void updateReason(String reason) {
        this.reason = reason;
    }

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

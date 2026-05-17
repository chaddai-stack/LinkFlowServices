package com.linkflow.api.risk.repository;

import com.linkflow.api.risk.domain.BlacklistEntryType;
import com.linkflow.api.risk.domain.RiskBlacklistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskBlacklistEntryRepository extends JpaRepository<RiskBlacklistEntry, UUID> {
    Optional<RiskBlacklistEntry> findByTypeAndValue(BlacklistEntryType type, String value);

    boolean existsByTypeAndValue(BlacklistEntryType type, String value);
}

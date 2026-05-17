package com.linkflow.api.risk.repository;

import com.linkflow.api.risk.domain.RiskScanTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RiskScanTaskRepository extends JpaRepository<RiskScanTask, UUID> {
}

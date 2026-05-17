package com.linkflow.api.risk.repository;

import com.linkflow.api.risk.domain.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RiskAlertRepository extends JpaRepository<RiskAlert, UUID>, JpaSpecificationExecutor<RiskAlert> {
}

package com.linkflow.api.link.repository;

import com.linkflow.api.link.domain.BulkLinkJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BulkLinkJobRepository extends JpaRepository<BulkLinkJob, UUID> {
}

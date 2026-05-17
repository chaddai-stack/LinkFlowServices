package com.linkflow.api.link.repository;

import com.linkflow.api.link.domain.LinkClassification;
import com.linkflow.api.link.domain.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LinkClassificationRepository extends JpaRepository<LinkClassification, UUID> {
    Optional<LinkClassification> findByLink(UrlMapping link);
}

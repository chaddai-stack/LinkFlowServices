package com.linkflow.api.link.repository;

import com.linkflow.api.link.domain.UrlMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long>, JpaSpecificationExecutor<UrlMapping> {
    Optional<UrlMapping> findFirstByLongUrlOrderByIdAsc(String longUrl);

    Optional<UrlMapping> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<UrlMapping> findAllByOrderByIdDesc(Pageable pageable);

    long countByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);
}

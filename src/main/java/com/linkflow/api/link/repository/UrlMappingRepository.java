package com.linkflow.api.link.repository;

import com.linkflow.api.link.domain.UrlMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 短链主表 仓库
 */
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long>, JpaSpecificationExecutor<UrlMapping> {
    /**
     * 按 longUrl 复用最早记录
     */
    Optional<UrlMapping> findFirstByLongUrlOrderByIdAsc(String longUrl);

    /**
     * 跳转链路按 back_half 查询
     */
    Optional<UrlMapping> findByBackHalf(String backHalf);

    /**
     * 新版 API 按 公开 UUID 查询
     */
    Optional<UrlMapping> findByPublicId(UUID publicId);

    boolean existsByBackHalf(String backHalf);

    /**
     * 临时热门链接查询
     */
    Page<UrlMapping> findAllByOrderByIdDesc(Pageable pageable);

    long countByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);

    long countByStatus(com.linkflow.api.link.domain.LinkStatus status);

    /**
     * 渠道维度统计
     */
    @Query("""
            select coalesce(mapping.channel, 'unknown'), count(mapping)
            from UrlMapping mapping
            group by coalesce(mapping.channel, 'unknown')
            order by count(mapping) desc
            """)
    java.util.List<Object[]> countByChannel();
}

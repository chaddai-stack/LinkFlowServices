package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.domain.LinkStatus;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.response.LinkSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LinkQueryService {

    private final UrlMappingRepository urlMappingRepository;
    private final String publicBaseUrl;

    public LinkQueryService(
            UrlMappingRepository urlMappingRepository,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * 默认分页查询
     */
    @Transactional(readOnly = true)
    public Page<LinkSummaryResponse> list(int page, int size) {
        return list(page, size, null, null, "created_at,desc");
    }

    /**
     * 按状态、关键字和排序条件查询短链
     *
     * search 使用 Specification 拼接动态条件；sort 使用白名单把 API 字段名转换成实体属性名。
     */
    @Transactional(readOnly = true)
    public Page<LinkSummaryResponse> list(int page, int size, String status, String search, String sort) {
        LinkStatus linkStatus = status == null || status.isBlank() ? null : LinkStatus.fromWireValue(status);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return urlMappingRepository.findAll(toSpecification(linkStatus, normalizedSearch), PageRequest.of(page - 1, size, parseSort(sort)))
                .map(mapping -> LinkSummaryResponse.from(mapping, publicBaseUrl));
    }

    /**
     * 返回 DTO 详情
     */
    @Transactional(readOnly = true)
    public LinkSummaryResponse getById(UUID linkId) {
        return LinkSummaryResponse.from(getMappingById(linkId), publicBaseUrl);
    }

    /**
     * 返回实体供其他服务 复用
     */
    @Transactional(readOnly = true)
    public UrlMapping getMappingById(UUID linkId) {
        return urlMappingRepository.findByPublicId(linkId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "LINK_NOT_FOUND",
                        "Short link does not exist or is inactive.",
                        Map.of("link_id", linkId)
                ));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",", 2);
        String property = switch (parts[0].trim()) {
            case "created_at", "createdAt" -> "createdAt";
            case "expires_at", "expiresAt" -> "expiresAt";
            case "title" -> "title";
            case "back_half" -> "backHalf";
            case "channel" -> "channel";
            case "status" -> "status";
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of("sort", "must be one of created_at, expires_at, title, back_half, channel, status")
            );
        };

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length == 2 && !parts[1].isBlank()) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ex) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        "Request validation failed.",
                        Map.of("sort", "direction must be asc or desc")
                );
            }
        }
        return Sort.by(direction, property);
    }

    private Specification<UrlMapping> toSpecification(LinkStatus status, String search) {
        return (root, query, criteriaBuilder) -> {
            // 从 conjunction 开始逐步 and 条件，便于 status/search 任意组合。
            var predicate = criteriaBuilder.conjunction();

            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("backHalf")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("longUrl")), pattern)
                        )
                );
            }

            return predicate;
        };
    }
}

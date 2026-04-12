package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.domain.LinkStatus;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.LinkSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
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

    public LinkQueryService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    @Transactional(readOnly = true)
    public Page<LinkSummaryResponse> list(int page, int size) {
        return list(page, size, null, null, "created_at,desc");
    }

    @Transactional(readOnly = true)
    public Page<LinkSummaryResponse> list(int page, int size, String status, String search, String sort) {
        LinkStatus linkStatus = status == null || status.isBlank() ? null : LinkStatus.fromWireValue(status);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return urlMappingRepository.findAll(toSpecification(linkStatus, normalizedSearch), PageRequest.of(page - 1, size, parseSort(sort)))
                .map(LinkSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public LinkSummaryResponse getById(UUID linkId) {
        return LinkSummaryResponse.from(getMappingById(linkId));
    }

    @Transactional(readOnly = true)
    public UrlMapping getMappingById(UUID linkId) {
        Long internalId = toInternalId(linkId);
        return urlMappingRepository.findById(internalId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "LINK_NOT_FOUND",
                        "Short link does not exist or is inactive.",
                        Map.of("link_id", linkId)
                ));
    }

    private Long toInternalId(UUID publicId) {
        if (publicId.getMostSignificantBits() != 0L || publicId.getLeastSignificantBits() <= 0L) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "LINK_NOT_FOUND",
                    "Short link does not exist or is inactive.",
                    Map.of("link_id", publicId)
            );
        }
        return publicId.getLeastSignificantBits();
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
            case "slug" -> "slug";
            case "channel" -> "channel";
            case "status" -> "status";
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of("sort", "must be one of created_at, expires_at, title, slug, channel, status")
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
            var predicate = criteriaBuilder.conjunction();

            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("longUrl")), pattern)
                        )
                );
            }

            return predicate;
        };
    }
}

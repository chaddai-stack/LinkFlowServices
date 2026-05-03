package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.domain.LinkStatus;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.request.CreateLinkRequest;
import com.linkflow.api.link.dto.request.UpdateLinkRequest;
import com.linkflow.api.link.dto.request.UpdateLinkStatusRequest;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

@Service
public class LinkCommandService {

    private static final int GENERATED_BACK_HALF_LENGTH = 7;
    private static final int MAX_RETRY = 10;
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final UrlMappingRepository urlMappingRepository;
    private final LinkTitleCrawlerService linkTitleCrawlerService;
    private final SecureRandom random = new SecureRandom();

    public LinkCommandService(
            UrlMappingRepository urlMappingRepository,
            LinkTitleCrawlerService linkTitleCrawlerService
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.linkTitleCrawlerService = linkTitleCrawlerService;
    }

    /**
     * 创建受管理的短链。
     */
    @Transactional
    public UrlMapping create(CreateLinkRequest request) {
        String backHalf = normalizeCustomBackHalf(request.customBackHalf());
        String title = linkTitleCrawlerService.resolveTitle(request.longUrl(), request.title());
        if (backHalf != null) {
            ensureBackHalfAvailable(backHalf);
            return saveOrConflict(new UrlMapping(
                    request.longUrl(),
                    backHalf,
                    title,
                    normalizeOptional(request.channel()),
                    request.expiresAt()
            ), backHalf);
        }

        var existing = urlMappingRepository.findFirstByLongUrlOrderByIdAsc(request.longUrl());
        if (existing.isPresent()) {
            return existing.get();
        }

        for (int i = 0; i < MAX_RETRY; i++) {
            String generatedBackHalf = generateBackHalf();
            try {
                return urlMappingRepository.save(new UrlMapping(
                        request.longUrl(),
                        generatedBackHalf,
                        title,
                        normalizeOptional(request.channel()),
                        request.expiresAt()
                ));
            } catch (DataIntegrityViolationException ex) {
                if (!urlMappingRepository.existsByBackHalf(generatedBackHalf)) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("failed to generate unique back_half after retries");
    }

    /**
     * 更新短链的可变字段。
     */
    @Transactional
    public UrlMapping update(UUID linkId, UpdateLinkRequest request) {
        UrlMapping mapping = getMapping(linkId);
        String nextBackHalf = normalizeCustomBackHalf(request.customBackHalf());
        if (nextBackHalf == null) {
            nextBackHalf = mapping.getBackHalf();
        } else if (!nextBackHalf.equals(mapping.getBackHalf())) {
            ensureBackHalfAvailable(nextBackHalf);
        }

        mapping.updateDetails(
                valueOrCurrent(request.longUrl(), mapping.getLongUrl()),
                nextBackHalf,
                valueOrCurrent(request.title(), mapping.getTitle()),
                valueOrCurrent(normalizeOptional(request.channel()), mapping.getChannel()),
                request.expiresAt() == null ? mapping.getExpiresAt() : request.expiresAt()
        );
        return mapping;
    }

    /**
     * 更新短链生命周期状态。
     */
    @Transactional
    public UrlMapping updateStatus(UUID linkId, UpdateLinkStatusRequest request) {
        UrlMapping mapping = getMapping(linkId);
        mapping.updateStatus(LinkStatus.fromWireValue(request.status()));
        return mapping;
    }

    /**
     * 按公开 link_id 删除短链。
     */
    @Transactional
    public void delete(UUID linkId) {
        UrlMapping mapping = getMapping(linkId);
        urlMappingRepository.delete(mapping);
    }

    private UrlMapping saveOrConflict(UrlMapping mapping, String backHalf) {
        try {
            return urlMappingRepository.save(mapping);
        } catch (DataIntegrityViolationException ex) {
            throw backHalfConflict(backHalf);
        }
    }

    private UrlMapping getMapping(UUID linkId) {
        return urlMappingRepository.findByPublicId(linkId)
                .orElseThrow(() -> linkNotFound(linkId));
    }

    private void ensureBackHalfAvailable(String backHalf) {
        if (urlMappingRepository.existsByBackHalf(backHalf)) {
            throw backHalfConflict(backHalf);
        }
    }

    private ApiException backHalfConflict(String backHalf) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "LINK_BACK_HALF_CONFLICT",
                "Short link back_half is already in use.",
                Map.of("custom_back_half", backHalf)
        );
    }

    private ApiException linkNotFound(UUID linkId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "LINK_NOT_FOUND",
                "Short link does not exist or is inactive.",
                Map.of("link_id", linkId)
        );
    }

    private String generateBackHalf() {
        StringBuilder sb = new StringBuilder(GENERATED_BACK_HALF_LENGTH);
        for (int i = 0; i < GENERATED_BACK_HALF_LENGTH; i++) {
            int idx = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(idx));
        }
        return sb.toString();
    }

    private String normalizeCustomBackHalf(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String valueOrCurrent(String nextValue, String currentValue) {
        return nextValue == null ? currentValue : nextValue;
    }
}

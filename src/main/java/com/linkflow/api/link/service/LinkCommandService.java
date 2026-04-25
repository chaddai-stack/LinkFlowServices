package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.domain.LinkStatus;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.CreateLinkRequest;
import com.linkflow.api.link.dto.UpdateLinkRequest;
import com.linkflow.api.link.dto.UpdateLinkStatusRequest;
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

    private static final int GENERATED_SLUG_LENGTH = 7;
    private static final int MAX_RETRY = 10;
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final UrlMappingRepository urlMappingRepository;
    private final SecureRandom random = new SecureRandom();

    public LinkCommandService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    @Transactional
    public UrlMapping create(CreateLinkRequest request) {
        String slug = normalizeCustomSlug(request.customSlug());
        if (slug != null) {
            ensureSlugAvailable(slug);
            return saveOrConflict(new UrlMapping(
                    request.longUrl(),
                    slug,
                    request.title(),
                    normalizeOptional(request.channel()),
                    request.expiresAt()
            ), slug);
        }

        for (int i = 0; i < MAX_RETRY; i++) {
            String generatedSlug = generateSlug();
            try {
                return urlMappingRepository.save(new UrlMapping(
                        request.longUrl(),
                        generatedSlug,
                        request.title(),
                        normalizeOptional(request.channel()),
                        request.expiresAt()
                ));
            } catch (DataIntegrityViolationException ex) {
                if (!urlMappingRepository.existsBySlug(generatedSlug)) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("failed to generate unique slug after retries");
    }

    @Transactional
    public UrlMapping update(UUID linkId, UpdateLinkRequest request) {
        UrlMapping mapping = getMapping(linkId);
        String nextSlug = normalizeCustomSlug(request.customSlug());
        if (nextSlug == null) {
            nextSlug = mapping.getSlug();
        } else if (!nextSlug.equals(mapping.getSlug())) {
            ensureSlugAvailable(nextSlug);
        }

        mapping.updateDetails(
                valueOrCurrent(request.longUrl(), mapping.getLongUrl()),
                nextSlug,
                valueOrCurrent(request.title(), mapping.getTitle()),
                valueOrCurrent(normalizeOptional(request.channel()), mapping.getChannel()),
                request.expiresAt() == null ? mapping.getExpiresAt() : request.expiresAt()
        );
        return mapping;
    }

    @Transactional
    public UrlMapping updateStatus(UUID linkId, UpdateLinkStatusRequest request) {
        UrlMapping mapping = getMapping(linkId);
        mapping.updateStatus(LinkStatus.fromWireValue(request.status()));
        return mapping;
    }

    @Transactional
    public void delete(UUID linkId) {
        UrlMapping mapping = getMapping(linkId);
        urlMappingRepository.delete(mapping);
    }

    private UrlMapping saveOrConflict(UrlMapping mapping, String slug) {
        try {
            return urlMappingRepository.save(mapping);
        } catch (DataIntegrityViolationException ex) {
            throw slugConflict(slug);
        }
    }

    private UrlMapping getMapping(UUID linkId) {
        Long internalId = toInternalId(linkId);
        return urlMappingRepository.findById(internalId)
                .orElseThrow(() -> linkNotFound(linkId));
    }

    private void ensureSlugAvailable(String slug) {
        if (urlMappingRepository.existsBySlug(slug)) {
            throw slugConflict(slug);
        }
    }

    private ApiException slugConflict(String slug) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "LINK_SLUG_CONFLICT",
                "Short link slug is already in use.",
                Map.of("custom_slug", slug)
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

    private Long toInternalId(UUID publicId) {
        if (publicId.getMostSignificantBits() != 0L || publicId.getLeastSignificantBits() <= 0L) {
            throw linkNotFound(publicId);
        }
        return publicId.getLeastSignificantBits();
    }

    private String generateSlug() {
        StringBuilder sb = new StringBuilder(GENERATED_SLUG_LENGTH);
        for (int i = 0; i < GENERATED_SLUG_LENGTH; i++) {
            int idx = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(idx));
        }
        return sb.toString();
    }

    private String normalizeCustomSlug(String value) {
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

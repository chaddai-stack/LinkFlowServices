package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.dto.LinkSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return urlMappingRepository.findAllByOrderByIdDesc(PageRequest.of(page - 1, size))
                .map(LinkSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public LinkSummaryResponse getById(UUID linkId) {
        Long internalId = toInternalId(linkId);
        return urlMappingRepository.findById(internalId)
                .map(LinkSummaryResponse::from)
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
}

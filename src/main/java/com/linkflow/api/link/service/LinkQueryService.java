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
    public LinkSummaryResponse getById(Long linkId) {
        return urlMappingRepository.findById(linkId)
                .map(LinkSummaryResponse::from)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "LINK_NOT_FOUND",
                        "Short link does not exist or is inactive.",
                        Map.of("link_id", linkId)
                ));
    }
}

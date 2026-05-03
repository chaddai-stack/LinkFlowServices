package com.linkflow.api.link.service;

import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class LinkRedirectService {

    private final UrlMappingRepository repository;

    public LinkRedirectService(UrlMappingRepository repository) {
        this.repository = repository;
    }

    /**
     * 根据 back-half 解析原始长链接
     */
    @Transactional(readOnly = true)
    public UrlMapping resolve(String backHalf) {
        return repository.findByBackHalf(backHalf)
                .orElseThrow(() -> new NoSuchElementException("short url not found"));
    }
}

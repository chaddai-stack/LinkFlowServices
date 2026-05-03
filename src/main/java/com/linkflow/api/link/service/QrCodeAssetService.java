package com.linkflow.api.link.service;

import com.linkflow.api.link.domain.LinkQrAsset;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.response.QrCodeAssetResponse;
import com.linkflow.api.link.repository.LinkQrAssetRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class QrCodeAssetService {

    private static final String FORMAT = "png";

    private final QrCodeService qrCodeService;
    private final LinkQrAssetRepository linkQrAssetRepository;
    private final S3ObjectStorageService s3ObjectStorageService;

    public QrCodeAssetService(
            QrCodeService qrCodeService,
            LinkQrAssetRepository linkQrAssetRepository,
            S3ObjectStorageService s3ObjectStorageService
    ) {
        this.qrCodeService = qrCodeService;
        this.linkQrAssetRepository = linkQrAssetRepository;
        this.s3ObjectStorageService = s3ObjectStorageService;
    }

    /**
     * 生成 PNG 并尝试存储资产
     *
     * 保留给未来“一次请求同时拿 PNG 与资产”的场景。
     */
    @Transactional
    public RenderedQrCode renderAndStore(UrlMapping mapping, String shortUrl, int size) {
        byte[] png = qrCodeService.generatePng(shortUrl, size);
        Optional<LinkQrAsset> asset = ensureStored(mapping, png, size);
        return new RenderedQrCode(png, asset.map(value -> toResponse(mapping, value)));
    }

    /**
     * 获取或创建云端二维码资产
     *
     * S3 未启用时返回空 Optional，由控制器映射成 data=null。
     */
    @Transactional
    public Optional<QrCodeAssetResponse> getOrCreateAsset(UrlMapping mapping, String shortUrl, int size) {
        Optional<LinkQrAsset> existing = findExisting(mapping, size);
        if (existing.isPresent()) {
            return existing.map(value -> toResponse(mapping, value));
        }
        if (!s3ObjectStorageService.isEnabled()) {
            return Optional.empty();
        }

        byte[] png = qrCodeService.generatePng(shortUrl, size);
        return ensureStored(mapping, png, size).map(value -> toResponse(mapping, value));
    }

    private Optional<LinkQrAsset> ensureStored(UrlMapping mapping, byte[] png, int size) {
        Optional<LinkQrAsset> existing = findExisting(mapping, size);
        if (existing.isPresent() || !s3ObjectStorageService.isEnabled()) {
            return existing;
        }

        String key = s3ObjectStorageService.qrCodeKey(mapping.getPublicId().toString(), size);
        S3ObjectStorageService.StoredObject stored = s3ObjectStorageService.putPng(key, png);
        try {
            return Optional.of(linkQrAssetRepository.save(new LinkQrAsset(
                    mapping.getId(),
                    FORMAT,
                    size,
                    stored.key(),
                    stored.url()
            )));
        } catch (DataIntegrityViolationException ex) {
            // 并发请求可能同时上传同一尺寸 二维码，唯一约束冲突后直接读取已存在记录。
            return findExisting(mapping, size);
        }
    }

    private Optional<LinkQrAsset> findExisting(UrlMapping mapping, int size) {
        return linkQrAssetRepository.findFirstByLinkIdAndFormatAndSizeOrderByCreatedAtDesc(
                mapping.getId(),
                FORMAT,
                size
        );
    }

    private QrCodeAssetResponse toResponse(UrlMapping mapping, LinkQrAsset asset) {
        return new QrCodeAssetResponse(
                asset.getId(),
                mapping.getPublicId(),
                asset.getFormat(),
                asset.getSize(),
                asset.getStorageKey(),
                asset.getStorageUrl(),
                asset.getCreatedAt()
        );
    }

    public record RenderedQrCode(byte[] png, Optional<QrCodeAssetResponse> asset) {
    }
}

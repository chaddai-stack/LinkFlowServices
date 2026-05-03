package com.linkflow.api.link.repository;

import com.linkflow.api.link.domain.LinkQrAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 二维码 资产 仓库
 */
public interface LinkQrAssetRepository extends JpaRepository<LinkQrAsset, UUID> {
    /**
     * 查询某条链接某种 二维码 规格的最新资产
     */
    Optional<LinkQrAsset> findFirstByLinkIdAndFormatAndSizeOrderByCreatedAtDesc(Long linkId, String format, int size);
}

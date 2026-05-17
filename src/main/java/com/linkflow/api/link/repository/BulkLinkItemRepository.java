package com.linkflow.api.link.repository;

import com.linkflow.api.link.domain.BulkLinkItem;
import com.linkflow.api.link.domain.BulkLinkJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BulkLinkItemRepository extends JpaRepository<BulkLinkItem, UUID> {

    /**
     * Bulk 结果需要直接展示短链信息，因此查询 item 时一起加载 link，避免响应组装阶段触发懒加载。
     */
    @EntityGraph(attributePaths = "link")
    List<BulkLinkItem> findByJobOrderByItemIndexAsc(BulkLinkJob job);
}

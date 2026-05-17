package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.dto.request.UpdateLinkStatusRequest;
import com.linkflow.api.link.dto.response.BulkLinkActionItemResponse;
import com.linkflow.api.link.dto.response.BulkLinkActionResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 对已创建链接执行批量操作。
 *
 * 这里刻意保持同步逐条执行：一条失败只写入该条结果，不拖垮整批。
 */
@Service
public class BulkLinkActionService {

    private final LinkCommandService linkCommandService;

    public BulkLinkActionService(LinkCommandService linkCommandService) {
        this.linkCommandService = linkCommandService;
    }

    public BulkLinkActionResponse updateStatus(List<UUID> linkIds, String status) {
        List<BulkLinkActionItemResponse> items = linkIds.stream()
                .map(linkId -> updateOneStatus(linkId, status))
                .toList();
        return BulkLinkActionResponse.from(items);
    }

    public BulkLinkActionResponse delete(List<UUID> linkIds) {
        List<BulkLinkActionItemResponse> items = linkIds.stream()
                .map(this::deleteOne)
                .toList();
        return BulkLinkActionResponse.from(items);
    }

    private BulkLinkActionItemResponse updateOneStatus(UUID linkId, String status) {
        try {
            linkCommandService.updateStatus(linkId, new UpdateLinkStatusRequest(status));
            return BulkLinkActionItemResponse.succeeded(linkId);
        } catch (ApiException ex) {
            return BulkLinkActionItemResponse.failed(linkId, ex.getCode(), ex.getMessage());
        } catch (RuntimeException ex) {
            return BulkLinkActionItemResponse.failed(linkId, "BULK_LINK_ACTION_FAILED", message(ex));
        }
    }

    private BulkLinkActionItemResponse deleteOne(UUID linkId) {
        try {
            linkCommandService.delete(linkId);
            return BulkLinkActionItemResponse.succeeded(linkId);
        } catch (ApiException ex) {
            return BulkLinkActionItemResponse.failed(linkId, ex.getCode(), ex.getMessage());
        } catch (RuntimeException ex) {
            return BulkLinkActionItemResponse.failed(linkId, "BULK_LINK_ACTION_FAILED", message(ex));
        }
    }

    private String message(RuntimeException ex) {
        return ex.getMessage() == null ? "Bulk link action failed." : ex.getMessage();
    }
}

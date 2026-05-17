package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.domain.BulkLinkJob;
import com.linkflow.api.link.domain.BulkLinkItem;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.request.BulkCreateLinksRequest;
import com.linkflow.api.link.dto.request.CreateLinkRequest;
import com.linkflow.api.link.dto.response.BulkLinkJobResponse;
import com.linkflow.api.link.repository.BulkLinkItemRepository;
import com.linkflow.api.link.repository.BulkLinkJobRepository;
import com.linkflow.api.risk.dto.RiskScanTaskRequest;
import com.linkflow.api.risk.dto.RiskScanTaskResponse;
import com.linkflow.api.risk.service.RiskService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BulkLinkService {

    private static final int MAX_ITEMS = 100;

    private final BulkLinkJobRepository jobRepository;
    private final BulkLinkItemRepository itemRepository;
    private final LinkCommandService linkCommandService;
    private final RiskService riskService;
    private final LinkClassificationService classificationService;
    private final Validator validator;
    private final String publicBaseUrl;

    public BulkLinkService(
            BulkLinkJobRepository jobRepository,
            BulkLinkItemRepository itemRepository,
            LinkCommandService linkCommandService,
            RiskService riskService,
            LinkClassificationService classificationService,
            Validator validator,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.jobRepository = jobRepository;
        this.itemRepository = itemRepository;
        this.linkCommandService = linkCommandService;
        this.riskService = riskService;
        this.classificationService = classificationService;
        this.validator = validator;
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * 同步执行 Bulk 最小闭环。
     *
     * 每条 item 独立处理：创建短链、风险检测、分类。任意单条失败只写入该 item 的错误信息，
     * 不影响同批其它链接，也不让整个 HTTP 请求失败。
     */
    public BulkLinkJobResponse create(BulkCreateLinksRequest request) {
        List<BulkCreateLinksRequest.BulkCreateLinkItemRequest> requestItems = request.items();
        if (requestItems == null || requestItems.isEmpty() || requestItems.size() > MAX_ITEMS) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of("items", "items size must be between 1 and 100")
            );
        }

        BulkLinkJob job = jobRepository.save(new BulkLinkJob(requestItems.size()));
        int succeeded = 0;
        int failed = 0;

        for (int i = 0; i < requestItems.size(); i++) {
            BulkLinkItem item = createPendingItem(job, i, requestItems.get(i));
            if (processItem(item, requestItems.get(i))) {
                succeeded++;
            } else {
                failed++;
            }
        }

        job.complete(succeeded, failed);
        jobRepository.save(job);
        return get(job.getId());
    }

    public BulkLinkJobResponse get(UUID jobId) {
        BulkLinkJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BULK_LINK_JOB_NOT_FOUND",
                        "Bulk link job does not exist.",
                        Map.of("job_id", jobId)
                ));
        return BulkLinkJobResponse.from(job, itemRepository.findByJobOrderByItemIndexAsc(job), publicBaseUrl);
    }

    private BulkLinkItem createPendingItem(
            BulkLinkJob job,
            int index,
            BulkCreateLinksRequest.BulkCreateLinkItemRequest request
    ) {
        return itemRepository.save(new BulkLinkItem(
                job,
                index,
                nullToEmpty(request.longUrl()),
                normalizeOptional(request.title()),
                normalizeOptional(request.customBackHalf()),
                normalizeOptional(request.channel())
        ));
    }

    private boolean processItem(BulkLinkItem item, BulkCreateLinksRequest.BulkCreateLinkItemRequest rawItem) {
        try {
            CreateLinkRequest createRequest = toCreateRequest(rawItem);
            validateCreateRequest(createRequest);

            UrlMapping link = linkCommandService.create(createRequest);
            RiskScanTaskResponse risk = riskService.createScanTask(new RiskScanTaskRequest(link.getPublicId(), null));
            var classification = classificationService.classify(link.getPublicId());
            item.markSucceeded(link, risk.riskLevel(), risk.riskScore(), classification.category());
            itemRepository.save(item);
            return true;
        } catch (ApiException ex) {
            item.markFailed(ex.getCode(), ex.getMessage());
            itemRepository.save(item);
            return false;
        } catch (Exception ex) {
            item.markFailed("BULK_ITEM_FAILED", ex.getMessage() == null ? "Bulk item failed." : ex.getMessage());
            itemRepository.save(item);
            return false;
        }
    }

    private CreateLinkRequest toCreateRequest(BulkCreateLinksRequest.BulkCreateLinkItemRequest item) {
        return new CreateLinkRequest(
                normalizeRequired(item.longUrl()),
                normalizeOptional(item.title()),
                normalizeOptional(item.customBackHalf()),
                normalizeOptional(item.channel()),
                item.expiresAt(),
                List.of(),
                Map.of()
        );
    }

    private void validateCreateRequest(CreateLinkRequest request) {
        var violations = validator.validate(request);
        if (violations.isEmpty()) {
            return;
        }

        String message = violations.stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(this::formatViolation)
                .findFirst()
                .orElse("invalid item");
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed.",
                Map.of("item", message)
        );
    }

    private String formatViolation(ConstraintViolation<CreateLinkRequest> violation) {
        return violation.getPropertyPath() + " " + violation.getMessage();
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.dto.request.UpdateLinkStatusRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class BulkLinkActionServiceTest {

    @Test
    void updateStatusKeepsSingleFailureInsideBulkResult() {
        LinkCommandService linkCommandService = Mockito.mock(LinkCommandService.class);
        BulkLinkActionService service = new BulkLinkActionService(linkCommandService);
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Mockito.doThrow(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "LINK_NOT_FOUND",
                        "Short link does not exist.",
                        Map.of("link_id", second)
                ))
                .when(linkCommandService)
                .updateStatus(eq(second), any(UpdateLinkStatusRequest.class));

        var response = service.updateStatus(List.of(first, second), "paused");

        assertEquals(2, response.total());
        assertEquals(1, response.succeeded());
        assertEquals(1, response.failed());
        assertEquals("succeeded", response.items().get(0).status());
        assertEquals("LINK_NOT_FOUND", response.items().get(1).errorCode());
    }
}

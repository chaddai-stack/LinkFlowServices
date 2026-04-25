package com.linkflow.api.link.repository;

import com.linkflow.api.link.dto.LinkSummaryResponse;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.service.LinkQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:url-mapping-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Transactional
class UrlMappingRepositoryTest {

    @Autowired
    private UrlMappingRepository repository;

    @Test
    void findAllByOrderByIdDescReturnsLatestFirst() {
        repository.save(new UrlMapping("https://example.com/first", "first01"));
        repository.save(new UrlMapping("https://example.com/second", "second1"));

        var result = repository.findAllByOrderByIdDesc(PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertEquals("second1", result.getContent().get(0).getSlug());
    }

    @Test
    void linkListQueryReturnsActiveLinkWithoutSearchParameter() {
        UrlMapping google = repository.save(new UrlMapping(
                "https://www.google.com",
                "google",
                "Google",
                "browser",
                null
        ));
        LinkQueryService service = new LinkQueryService(repository);

        var result = service.list(1, 20, null, null, "created_at,desc");

        assertEquals(1, result.getTotalElements());
        LinkSummaryResponse item = result.getContent().get(0);
        assertEquals(LinkSummaryResponse.toPublicId(google.getId()), item.id());
        assertEquals("google", item.slug());
        assertEquals("Google", item.title());
        assertEquals("browser", item.channel());
        assertEquals("active", item.status());
    }

    @Test
    void linkListQueryCanFilterByStatusAndSearch() {
        repository.save(new UrlMapping(
                "https://www.google.com",
                "google",
                "Google",
                "browser",
                null
        ));
        repository.save(new UrlMapping(
                "https://example.com/other",
                "other",
                "Other",
                "email",
                null
        ));
        LinkQueryService service = new LinkQueryService(repository);

        var result = service.list(1, 20, "active", "goo", "created_at,desc");

        assertEquals(1, result.getTotalElements());
        assertEquals("google", result.getContent().get(0).slug());
    }
}

package com.linkflow.api.link.repository;

import com.linkflow.api.link.domain.UrlMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
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
}

package com.flipkart.crawl.ingestion.topology.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ValidationUtilTest {

    @Test
    void validProductId_passes() {
        assertNull(ValidationUtil.validateRawEventJson("{\"product_id\": 100}"));
    }

    @Test
    void missingProductId_fails() {
        assertNotNull(ValidationUtil.validateRawEventJson("{\"site_id\": 1}"));
    }

    @Test
    void empty_fails() {
        assertNotNull(ValidationUtil.validateRawEventJson(""));
    }

    @Test
    void invalidJson_fails() {
        assertNotNull(ValidationUtil.validateRawEventJson("{"));
    }
}

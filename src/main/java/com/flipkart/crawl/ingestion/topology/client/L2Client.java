package com.flipkart.crawl.ingestion.topology.client;

import com.flipkart.crawl.ingestion.topology.config.L2ClientConfig;

import java.util.Collections;
import java.util.List;

/**
 * Calls Layer 2 enrichment. Implementations should send identifier-only requests derived from the crawl JSON.
 */
public interface L2Client {

    /**
     * Batch call: one HTTP request with multiple {@code IdRef} entries (order preserved).
     * Response JSON should contain {@code enriched_events} array aligned with {@code rawEventJsons}.
     */
    String enrichBatch(List<String> rawEventJsons, L2ClientConfig cfg) throws Exception;

    /**
     * @param rawEventJson inner crawl event JSON (identifiers extracted by the client)
     */
    default String enrich(String rawEventJson, L2ClientConfig cfg) throws Exception {
        return enrichBatch(Collections.singletonList(rawEventJson), cfg);
    }
}
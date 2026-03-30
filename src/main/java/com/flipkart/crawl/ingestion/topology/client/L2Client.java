package com.flipkart.crawl.ingestion.topology.client;

import com.flipkart.crawl.ingestion.topology.config.L2ClientConfig;

/**
 * Calls Layer 2 enrichment. Implementations should send identifier-only requests derived from the crawl JSON.
 */
public interface L2Client {

    /**
     * @param rawEventJson inner crawl event JSON (identifiers extracted by the client)
     */
    String enrich(String rawEventJson, L2ClientConfig cfg) throws Exception;
}
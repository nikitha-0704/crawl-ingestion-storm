package com.flipkart.crawl.ingestion.topology.config;

import lombok.Data;

import java.io.Serializable;

/**
 * Topic names (and optional producer URL) for bolts that publish to Pulsar.
 * Spout-side settings stay on {@code PulsarClientConfig} / {@code PulsarSpoutConfig} (viesti).
 */
@Data
public class PulsarConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Full topic, e.g. persistent://ci-preprod/ci-crawl-enricher/ci-enriched-events-preprod */
    private String centralEnrichedTopic;

    /** Full topic for L1 retry loop */
    private String retryTopic;

    /** Full topic for L1 DLQ */
    private String dlqTopic;

    /**
     * Optional: if producers must use a different broker URL than spout client.
     * Leave null to reuse {@code EnrichmentTopologyConfig#pulsarClientConfig} serviceUrl.
     */
    private String producerServiceUrl;
}
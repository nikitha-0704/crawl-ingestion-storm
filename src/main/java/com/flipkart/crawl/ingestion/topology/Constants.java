package com.flipkart.crawl.ingestion.topology;

/**
 * Tuple field names and internal stream ids for Layer 1 topology.
 */
public final class Constants {

    private Constants() {}

    /** Used for fieldsGrouping across spouts → enrich → central (e.g. product_id or stable dedupe key). */
    public static final String KEY = "KEY";

    /** Raw JSON string from Pulsar (before L2). */
    public static final String RAW_EVENT = "RAW_EVENT";

    /** Enriched payload JSON after L2 (before central publish). */
    public static final String ENRICHED_EVENT = "ENRICHED_EVENT";

    /** Stream from AsyncEnrichmentBolt to RetryBolt. */
    public static final String RETRY_STREAM = "RETRY_STREAM";

    /** Stream from RetryBolt to DLQBolt. */
    public static final String DLQ_STREAM = "DLQ_STREAM";

    /** Guice map name for viesti {@code PulsarMessageProducer} instances (enriched-order-consumer pattern). */
    public static final String PULSAR_PRODUCERS_MAP = "pulsarProducersMap";

    public static final String CENTRAL_PULSAR_PRODUCER = "centralEnrichedPulsarProducer";
    public static final String RETRY_PULSAR_PRODUCER = "retryPulsarProducer";
    public static final String DLQ_PULSAR_PRODUCER = "dlqPulsarProducer";
}
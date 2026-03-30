package com.flipkart.crawl.ingestion.topology.metrics;

/**
 * Stable metric name fragments; Storm will prefix by component/task.
 */
public final class MetricsNames {

    private MetricsNames() {}

    public static final String ENRICH_SUCCESS = "crawl.enrich.success";
    public static final String ENRICH_FAILURE = "crawl.enrich.failure";
    public static final String ENRICH_RETRY_EMIT = "crawl.enrich.retry_emit";
    public static final String ENRICH_DLQ_EMIT = "crawl.enrich.dlq_emit";

    public static final String CENTRAL_PUBLISH_SUCCESS = "crawl.central.publish.success";
    public static final String CENTRAL_PUBLISH_FAILURE = "crawl.central.publish.failure";

    public static final String RETRY_PUBLISH_SUCCESS = "crawl.retry.publish.success";
    public static final String RETRY_PUBLISH_FAILURE = "crawl.retry.publish.failure";

    public static final String DLQ_PUBLISH_SUCCESS = "crawl.dlq.publish.success";
    public static final String DLQ_PUBLISH_FAILURE = "crawl.dlq.publish.failure";

    public static final String VALIDATION_REJECT = "crawl.validation.reject";
}
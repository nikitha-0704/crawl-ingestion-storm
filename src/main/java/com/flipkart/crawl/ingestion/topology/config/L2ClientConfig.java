package com.flipkart.crawl.ingestion.topology.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class L2ClientConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String baseUrl;
    /** e.g. /v1/enrich/batch or team-standard path */
    private String enrichPath = "/enrich";

    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 10000;

    /** Align with design: caps identifiers per L2 HTTP request (also sub-batches large Storm windows). */
    private int maxBatchSize = 30;

    /**
     * Storm tumbling window length in tuples when {@code > 0}. Default 30 matches micro-batch design.
     * Set to 0 to use {@link #microBatchWindowSeconds} (time tumbling) instead.
     */
    private int microBatchWindowCount = 30;

    /** Used when {@code microBatchWindowCount <= 0}: emit window on this interval (processing time). */
    private int microBatchWindowSeconds = 30;
}
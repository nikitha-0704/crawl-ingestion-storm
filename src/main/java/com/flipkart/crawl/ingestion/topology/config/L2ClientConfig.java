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

    /** Align with design: 30 events or 30s in bolt; this caps batch size for HTTP body. */
    private int maxBatchSize = 30;
}
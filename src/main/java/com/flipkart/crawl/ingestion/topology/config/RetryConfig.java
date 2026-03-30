package com.flipkart.crawl.ingestion.topology.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class RetryConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** After this many attempts, route to DLQ (diagram: 3). */
    private int maxAttempts = 3;
}
package com.flipkart.crawl.ingestion.topology.model;

import lombok.Data;

@Data
public class RetryEnvelope {
    private String rawEventJson;
    private int attempt;
}
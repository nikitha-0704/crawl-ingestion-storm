package com.flipkart.crawl.ingestion.topology.model;

import lombok.Data;

import java.util.Map;

@Data
public class EnrichedEvent {
    private RawEvent raw;
    private String match_status;
    private Map<String, Object> routing_tags;
    private Map<String, Object> fk_data;
}
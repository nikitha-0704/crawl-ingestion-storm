package com.flipkart.crawl.ingestion.topology.model;

import lombok.Data;
import java.util.List;

@Data
public class EnrichResponse {
    private List<EnrichedEvent> enriched_events;
}
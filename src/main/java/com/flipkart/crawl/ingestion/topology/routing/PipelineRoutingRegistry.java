package com.flipkart.crawl.ingestion.topology.routing;

import java.util.Collections;
import java.util.List;

/**
 * In-memory (site_id, competitor_id) → pipeline names for Layer-3 routing. Populated from JDBC when enabled.
 */
public interface PipelineRoutingRegistry {

    List<String> pipelinesFor(Integer siteId, Integer competitorId);

    PipelineRoutingRegistry NOOP = (siteId, competitorId) -> Collections.emptyList();
}

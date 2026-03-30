package com.flipkart.crawl.ingestion.topology.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * Batch-shaped request body with identifier-only events for L2.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrichIdsRequest {

    private List<IdRef> events;
}

package com.flipkart.crawl.ingestion.topology.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.crawl.ingestion.topology.util.JsonUtil;
import lombok.Data;

import java.io.IOException;

/**
 * Identifier fields only — sent to Layer 2 per architecture (no full crawl payload).
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdRef {

    private Long product_id;
    private Integer site_id;
    private Integer competitor_id;
    private String sku;
    private Integer avail_id;

    /** Build from crawl JSON; ignores unknown fields. */
    public static IdRef fromCrawlJson(String crawlRawJson) throws IOException {
        JsonNode n = JsonUtil.MAPPER.readTree(crawlRawJson);
        IdRef id = new IdRef();
        if (n == null || n.isNull()) {
            return id;
        }
        if (n.hasNonNull("product_id") && n.get("product_id").isNumber()) {
            id.setProduct_id(n.get("product_id").asLong());
        }
        if (n.hasNonNull("site_id") && n.get("site_id").isNumber()) {
            id.setSite_id(n.get("site_id").asInt());
        }
        if (n.hasNonNull("competitor_id") && n.get("competitor_id").isNumber()) {
            id.setCompetitor_id(n.get("competitor_id").asInt());
        }
        if (n.hasNonNull("sku") && n.get("sku").isTextual()) {
            id.setSku(n.get("sku").asText());
        }
        if (n.hasNonNull("avail_id") && n.get("avail_id").isNumber()) {
            id.setAvail_id(n.get("avail_id").asInt());
        }
        return id;
    }
}

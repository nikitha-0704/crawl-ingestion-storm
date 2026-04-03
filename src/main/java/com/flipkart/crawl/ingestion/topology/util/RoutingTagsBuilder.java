package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.crawl.ingestion.topology.routing.PipelineRoutingRegistry;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer-1 routing tags attached in the central publisher (diagram: routing tags on consolidated payload).
 */
public final class RoutingTagsBuilder {

    private RoutingTagsBuilder() {}

    /** Derive tags from crawl JSON (same keys as crawl event). No DB-backed pipelines. */
    public static Map<String, Object> fromCrawlJson(String crawlRawJson) throws IOException {
        return fromCrawlJson(crawlRawJson, PipelineRoutingRegistry.NOOP);
    }

    /**
     * Adds {@code pipelines} from in-memory (site_id, competitor_id) classifier when the registry has a mapping.
     */
    public static Map<String, Object> fromCrawlJson(String crawlRawJson, PipelineRoutingRegistry registry) throws IOException {
        Map<String, Object> tags = new LinkedHashMap<>();
        tags.put("source", "CRAWL");
        PipelineRoutingRegistry reg = registry != null ? registry : PipelineRoutingRegistry.NOOP;
        if (crawlRawJson == null || crawlRawJson.isEmpty()) {
            return tags;
        }
        JsonNode n = JsonUtil.MAPPER.readTree(crawlRawJson);
        if (n == null || n.isNull()) {
            return tags;
        }
        putNumber(tags, "site_id", n, "site_id");
        putNumber(tags, "competitor_id", n, "competitor_id");
        putNumber(tags, "product_id", n, "product_id");
        putText(tags, "sku", n, "sku");
        putText(tags, "category", n, "category");
        Integer siteId = intOrNull(n, "site_id");
        Integer compId = intOrNull(n, "competitor_id");
        List<String> pipelines = reg.pipelinesFor(siteId, compId);
        if (!pipelines.isEmpty()) {
            tags.put("pipelines", pipelines);
        }
        return tags;
    }

    private static Integer intOrNull(JsonNode root, String field) {
        if (root == null || !root.hasNonNull(field) || !root.get(field).isNumber()) {
            return null;
        }
        return root.get(field).intValue();
    }

    private static void putNumber(Map<String, Object> tags, String outKey, JsonNode root, String field) {
        if (root.hasNonNull(field) && root.get(field).isNumber()) {
            tags.put(outKey, root.get(field).numberValue());
        }
    }

    private static void putText(Map<String, Object> tags, String outKey, JsonNode root, String field) {
        if (root.hasNonNull(field) && root.get(field).isTextual()) {
            tags.put(outKey, root.get(field).asText());
        }
    }
}

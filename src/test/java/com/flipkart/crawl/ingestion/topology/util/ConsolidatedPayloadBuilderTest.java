package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.crawl.ingestion.topology.routing.PipelineRoutingRegistry;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsolidatedPayloadBuilderTest {

    private static final String CRAWL = "{\"product_id\":7,\"site_id\":2,\"competitor_id\":3}";

    @Test
    void build_mergesRoutingTagsAndL2SingleObject() throws Exception {
        String l2 = "{\"match_status\":\"MATCHED\",\"fk_data\":{\"x\":1}}";
        String out = ConsolidatedPayloadBuilder.build(CRAWL, l2);
        JsonNode root = JsonUtil.MAPPER.readTree(out);
        assertEquals("MATCHED", root.get("match_status").asText());
        assertEquals("CRAWL", root.get("routing_tags").get("source").asText());
        assertEquals(7, root.get("routing_tags").get("product_id").asInt());
        assertEquals(1, root.get("fk_data").get("x").asInt());
    }

    @Test
    void build_enrichedEventsArray_usesFirst() throws Exception {
        String l2 = "{\"enriched_events\":[{\"match_status\":\"OK\",\"fk_data\":{}}]}";
        String out = ConsolidatedPayloadBuilder.build(CRAWL, l2);
        JsonNode root = JsonUtil.MAPPER.readTree(out);
        assertEquals("OK", root.get("match_status").asText());
    }

    @Test
    void build_unknownL2_wrapsInFkData() throws Exception {
        String l2 = "{\"foo\":true}";
        String out = ConsolidatedPayloadBuilder.build(CRAWL, l2);
        JsonNode root = JsonUtil.MAPPER.readTree(out);
        assertTrue(root.get("fk_data").get("l2_body").has("foo"));
    }

    @Test
    void build_includesPipelinesFromRegistry() throws Exception {
        String l2 = "{\"match_status\":\"MATCHED\",\"fk_data\":{}}";
        PipelineRoutingRegistry reg = (s, c) ->
                s != null && s == 2 && c != null && c == 3 ? Collections.singletonList("CI") : Collections.emptyList();
        String out = ConsolidatedPayloadBuilder.build(CRAWL, l2, reg);
        JsonNode root = JsonUtil.MAPPER.readTree(out);
        assertEquals("CI", root.get("routing_tags").get("pipelines").get(0).asText());
    }
}

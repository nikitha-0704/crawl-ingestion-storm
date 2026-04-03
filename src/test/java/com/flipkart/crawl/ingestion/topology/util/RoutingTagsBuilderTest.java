package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.crawl.ingestion.topology.routing.PipelineRoutingRegistry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RoutingTagsBuilderTest {

    private static final String CRAWL = "{\"product_id\":7,\"site_id\":6025,\"competitor_id\":6002}";

    @Test
    void fromCrawlJson_addsPipelinesWhenRegistryMatches() throws Exception {
        PipelineRoutingRegistry reg = (site, comp) ->
                site != null && site == 6025 && comp != null && comp == 6002
                        ? Arrays.asList("CI", "BANK_OFFER")
                        : Collections.emptyList();
        Map<String, Object> tags = RoutingTagsBuilder.fromCrawlJson(CRAWL, reg);
        assertEquals("CRAWL", tags.get("source"));
        @SuppressWarnings("unchecked")
        java.util.List<String> pipes = (java.util.List<String>) tags.get("pipelines");
        assertEquals(Arrays.asList("CI", "BANK_OFFER"), pipes);
    }

    @Test
    void fromCrawlJson_omitsPipelinesWhenNoMatch() throws Exception {
        Map<String, Object> tags = RoutingTagsBuilder.fromCrawlJson(CRAWL, PipelineRoutingRegistry.NOOP);
        assertFalse(tags.containsKey("pipelines"));
    }

    @Test
    void fromCrawlJson_serializesPipelinesAsJsonArray() throws Exception {
        PipelineRoutingRegistry reg = (s, c) -> Collections.singletonList("MEESHO");
        String json = JsonUtil.MAPPER.writeValueAsString(RoutingTagsBuilder.fromCrawlJson(CRAWL, reg));
        JsonNode n = JsonUtil.MAPPER.readTree(json);
        assertEquals("MEESHO", n.get("pipelines").get(0).asText());
    }
}

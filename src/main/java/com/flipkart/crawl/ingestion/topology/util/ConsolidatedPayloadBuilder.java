package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.crawl.ingestion.topology.model.EnrichedEvent;
import com.flipkart.crawl.ingestion.topology.model.RawEvent;
import com.flipkart.crawl.ingestion.topology.routing.PipelineRoutingRegistry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merges crawl raw event with L2 response into one consolidated enriched payload for {@code central-enriched-events}.
 */
public final class ConsolidatedPayloadBuilder {

    private ConsolidatedPayloadBuilder() {}

    public static String build(String crawlRawJson, String l2ResponseJson) throws Exception {
        return build(crawlRawJson, l2ResponseJson, PipelineRoutingRegistry.NOOP);
    }

    public static String build(String crawlRawJson, String l2ResponseJson, PipelineRoutingRegistry pipelineRegistry)
            throws Exception {
        RawEvent raw = RawEventParser.parseLenient(crawlRawJson);
        EnrichedEvent out = new EnrichedEvent();
        out.setRaw(raw);
        out.setRouting_tags(RoutingTagsBuilder.fromCrawlJson(crawlRawJson, pipelineRegistry));

        JsonNode root = JsonUtil.MAPPER.readTree(l2ResponseJson);
        if (root == null || root.isNull()) {
            out.setMatch_status("UNKNOWN");
            return JsonUtil.MAPPER.writeValueAsString(out);
        }

        if (root.hasNonNull("enriched_events") && root.get("enriched_events").isArray()
                && root.get("enriched_events").size() > 0) {
            applyL2Node(out, root.get("enriched_events").get(0));
        } else if (root.has("match_status") || root.has("fk_data") || root.has("routing_tags")) {
            applyL2Node(out, root);
        } else {
            Map<String, Object> fk = new HashMap<>(1);
            fk.put("l2_body", JsonUtil.MAPPER.readValue(l2ResponseJson, Object.class));
            out.setFk_data(fk);
            out.setMatch_status("UNPARSED_L2");
        }

        return JsonUtil.MAPPER.writeValueAsString(out);
    }

    private static void applyL2Node(EnrichedEvent target, JsonNode node) {
        if (node.hasNonNull("match_status")) {
            target.setMatch_status(node.get("match_status").asText());
        }
        if (node.has("fk_data")) {
            target.setFk_data(jsonObjectToMap(node.get("fk_data")));
        }
        if (node.has("routing_tags")) {
            Map<String, Object> l2Tags = jsonObjectToMap(node.get("routing_tags"));
            Map<String, Object> merged = new LinkedHashMap<>(target.getRouting_tags() != null
                    ? target.getRouting_tags()
                    : new LinkedHashMap<>());
            merged.putAll(l2Tags);
            target.setRouting_tags(merged);
        }
    }

    private static Map<String, Object> jsonObjectToMap(JsonNode n) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (n == null || !n.isObject()) {
            return map;
        }
        for (Iterator<String> it = n.fieldNames(); it.hasNext(); ) {
            String field = it.next();
            map.put(field, JsonUtil.MAPPER.convertValue(n.get(field), Object.class));
        }
        return map;
    }
}

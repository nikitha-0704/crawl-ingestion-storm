package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Slices a batch L2 JSON body so {@link ConsolidatedPayloadBuilder} can consume one event at a time.
 */
public final class L2ResponseSlicer {

    private L2ResponseSlicer() {}

    /** {@code enriched_events} array node, or null if absent / not an array. */
    public static JsonNode enrichedEventsArray(String l2FullResponseJson) throws Exception {
        if (l2FullResponseJson == null || l2FullResponseJson.isEmpty()) {
            return null;
        }
        JsonNode root = JsonUtil.MAPPER.readTree(l2FullResponseJson);
        if (root != null && root.hasNonNull("enriched_events") && root.get("enriched_events").isArray()) {
            return root.get("enriched_events");
        }
        return null;
    }

    /**
     * Produces a JSON object with a single-element {@code enriched_events} array for index {@code i},
     * or the full body when there is no array and {@code i == 0} (legacy L2 shape).
     */
    public static String sliceForTuple(String l2FullResponseJson, int i, JsonNode enrichedArray) throws Exception {
        if (enrichedArray != null && i >= 0 && i < enrichedArray.size()) {
            ObjectNode wrap = JsonUtil.MAPPER.createObjectNode();
            ArrayNode arr = JsonUtil.MAPPER.createArrayNode();
            arr.add(enrichedArray.get(i));
            wrap.set("enriched_events", arr);
            return JsonUtil.MAPPER.writeValueAsString(wrap);
        }
        if (i == 0 && enrichedArray == null) {
            return l2FullResponseJson;
        }
        ObjectNode wrap = JsonUtil.MAPPER.createObjectNode();
        wrap.set("enriched_events", JsonUtil.MAPPER.createArrayNode());
        return JsonUtil.MAPPER.writeValueAsString(wrap);
    }
}

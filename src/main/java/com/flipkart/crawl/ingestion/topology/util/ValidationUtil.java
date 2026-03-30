package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Light validation for crawl raw events (JSON already parsed as tree).
 * Tighten rules (required fields, ranges) as your contract stabilizes.
 */
public final class ValidationUtil {

    private ValidationUtil() {}

    public static boolean hasProductId(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }
        JsonNode id = root.get("product_id");
        return id != null && id.isNumber() && id.asLong() > 0;
    }

    public static String validateRawEventJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "empty payload";
        }
        try {
            JsonNode root = JsonUtil.MAPPER.readTree(json);
            if (!hasProductId(root)) {
                return "missing or invalid product_id";
            }
            return null;
        } catch (Exception e) {
            return "invalid json: " + e.getMessage();
        }
    }
}
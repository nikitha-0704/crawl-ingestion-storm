package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {
    public static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonUtil() {}
}
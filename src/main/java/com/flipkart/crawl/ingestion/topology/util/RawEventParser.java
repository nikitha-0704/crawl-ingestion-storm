package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.crawl.ingestion.topology.model.RawEvent;

import java.io.IOException;

public final class RawEventParser {

    private static final ObjectMapper LENIENT = createLenient();

    private static ObjectMapper createLenient() {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return m;
    }

    private RawEventParser() {}

    public static RawEvent parseLenient(String json) throws IOException {
        return LENIENT.readValue(json, RawEvent.class);
    }
}

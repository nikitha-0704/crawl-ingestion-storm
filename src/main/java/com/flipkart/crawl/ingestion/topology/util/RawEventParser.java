package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.flipkart.crawl.ingestion.topology.model.RawEvent;

import java.io.IOException;

public final class RawEventParser {

    private static final ObjectReader LENIENT =
            JsonUtil.MAPPER.readerFor(RawEvent.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private RawEventParser() {}

    public static RawEvent parseLenient(String json) throws IOException {
        return LENIENT.readValue(json);
    }
}

package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.crawl.ingestion.topology.config.RetryConfig;
import com.flipkart.crawl.ingestion.topology.model.RetryEnvelope;

/**
 * Helpers for retry topic payloads: parse envelope, bump attempt, decide DLQ vs retry.
 */
public final class RetryUtil {

    private RetryUtil() {}

    /**
     * True if JSON looks like {@link RetryEnvelope} (has textual {@code rawEventJson}).
     * Avoids mis-parsing arbitrary crawl payloads that deserialize to empty envelope fields.
     */
    public static boolean looksLikeEnvelope(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            JsonNode n = JsonUtil.MAPPER.readTree(json);
            return n != null
                    && n.hasNonNull("rawEventJson")
                    && n.get("rawEventJson").isTextual();
        } catch (Exception e) {
            return false;
        }
    }

    /** Inner raw JSON for L2, or the full string if the tuple is not an envelope. */
    public static String extractRawEventJson(String tuplePayload) {
        RetryEnvelope env = tryEnvelope(tuplePayload);
        return env != null ? env.getRawEventJson() : tuplePayload;
    }

    /** Attempt count from envelope, or 0 for plain raw payloads. */
    public static int currentAttempt(String tuplePayload) {
        RetryEnvelope env = tryEnvelope(tuplePayload);
        return env != null ? env.getAttempt() : 0;
    }

    /** Serialized envelope for the retry topic after a failed enrichment. */
    public static String wrapWithAttempt(String rawEventJson, int attempt) throws JsonProcessingException {
        RetryEnvelope e = new RetryEnvelope();
        e.setRawEventJson(rawEventJson);
        e.setAttempt(attempt);
        return serialize(e);
    }

    private static RetryEnvelope tryEnvelope(String tuplePayload) {
        if (tuplePayload == null || tuplePayload.isEmpty() || !looksLikeEnvelope(tuplePayload)) {
            return null;
        }
        try {
            RetryEnvelope e = parse(tuplePayload);
            if (e.getRawEventJson() == null || e.getRawEventJson().isEmpty()) {
                return null;
            }
            return e;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static RetryEnvelope parse(String json) throws JsonProcessingException {
        if (json == null || json.isEmpty()) {
            throw new IllegalArgumentException("retry payload is empty");
        }
        return JsonUtil.MAPPER.readValue(json, RetryEnvelope.class);
    }

    public static String serialize(RetryEnvelope envelope) throws JsonProcessingException {
        return JsonUtil.MAPPER.writeValueAsString(envelope);
    }

    /** First failure: wrap raw JSON as attempt 1. */
    public static String wrapFirstAttempt(String rawEventJson) throws JsonProcessingException {
        RetryEnvelope e = new RetryEnvelope();
        e.setRawEventJson(rawEventJson);
        e.setAttempt(1);
        return serialize(e);
    }

    /** Increment attempt after a failed enrichment. */
    public static String bumpAttempt(String envelopeJson) throws JsonProcessingException {
        RetryEnvelope e = parse(envelopeJson);
        e.setAttempt(e.getAttempt() + 1);
        return serialize(e);
    }

    public static boolean shouldDlq(RetryEnvelope envelope, RetryConfig cfg) {
        int max = cfg != null && cfg.getMaxAttempts() > 0 ? cfg.getMaxAttempts() : 3;
        return envelope.getAttempt() >= max;
    }

    public static String rawFromEnvelope(String envelopeJson) throws JsonProcessingException {
        return parse(envelopeJson).getRawEventJson();
    }
}
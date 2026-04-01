package com.flipkart.crawl.ingestion.topology.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.flipkart.pricing.commons.viesti_commons.config.PulsarClientConfig;
import org.flipkart.pricing.commons.viesti_commons.util.AuthUtils;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Validates viesti producer props and injects OAuth params — same pattern as enriched-order-consumer.
 */
@Slf4j
public final class PulsarValidations {

    private PulsarValidations() {}

    public static Map<String, Object> validatePulsarProperties(Map<String, Object> props, PulsarClientConfig pulsarClientConfig) {
        if (pulsarClientConfig == null || props == null || props.isEmpty()) {
            log.warn("Pulsar configuration is incomplete. pulsarClientConfig present: {}, producerProperties present: {}",
                    pulsarClientConfig != null, props != null && !props.isEmpty());
            return null;
        }
        if (!props.containsKey("bootstrap.servers")) {
            log.error("Missing required 'bootstrap.servers' in Pulsar producer configuration");
            return null;
        }
        if (!props.containsKey("key.serializer") || !props.containsKey("value.serializer")) {
            log.error("Missing required serializer properties in Pulsar producer configuration");
            return null;
        }
        Map<String, Object> copy = new HashMap<>(props);
        try {
            String authParams = AuthUtils.getAuthParams(pulsarClientConfig);
            if (authParams != null && !authParams.isEmpty()) {
                copy.put("pulsar.authentication.params.string", authParams);
                log.info("Added Pulsar authentication parameters for client: {}", pulsarClientConfig.getClientId());
            } else {
                log.error("AuthUtils.getAuthParams returned null or empty string");
                throw new IllegalStateException("Pulsar authentication parameters are missing or empty.");
            }
        } catch (JsonProcessingException | MalformedURLException e) {
            log.warn("Failed to generate Pulsar authentication parameters: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate Pulsar authentication parameters", e);
        }
        return copy;
    }
}

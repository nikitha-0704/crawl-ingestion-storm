package com.flipkart.crawl.ingestion.topology.guice;

import com.codahale.metrics.MetricRegistry;
import com.flipkart.crawl.ingestion.topology.Constants;
import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.crawl.ingestion.topology.config.PulsarConfig;
import com.flipkart.crawl.ingestion.topology.util.PulsarValidations;
import lombok.extern.slf4j.Slf4j;
import org.flipkart.pricing.commons.viesti_commons.config.PulsarClientConfig;
import org.flipkart.pricing.commons.viesti_commons.producer.PulsarMessageProducer;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds {@link PulsarMessageProducer} instances from topology config (enriched-order-consumer pattern).
 */
@Slf4j
public final class PulsarProducerFactory {

    private PulsarProducerFactory() {}

    public static Map<String, PulsarMessageProducer> build(EnrichmentTopologyConfig cfg, MetricRegistry metricRegistry) {
        Map<String, PulsarMessageProducer> producers = new HashMap<>();
        PulsarClientConfig clientConfig = cfg.getPulsarClientConfig();
        Map<String, Object> baseProps = cfg.getPulsarProducerProps();
        PulsarConfig topics = cfg.getPulsarTopics();

        if (topics == null) {
            log.warn("pulsarTopics not configured; no Pulsar producers created.");
            return producers;
        }

        addIfPresent(producers, Constants.CENTRAL_PULSAR_PRODUCER,
                createProducer("central", baseProps, topics.getCentralEnrichedTopic(), clientConfig, metricRegistry));
        addIfPresent(producers, Constants.RETRY_PULSAR_PRODUCER,
                createProducer("retry", baseProps, topics.getRetryTopic(), clientConfig, metricRegistry));
        addIfPresent(producers, Constants.DLQ_PULSAR_PRODUCER,
                createProducer("dlq", baseProps, topics.getDlqTopic(), clientConfig, metricRegistry));

        return producers;
    }

    private static void addIfPresent(Map<String, PulsarMessageProducer> map, String key, PulsarMessageProducer p) {
        if (p != null) {
            map.put(key, p);
        }
    }

    private static PulsarMessageProducer createProducer(
            String logicalName,
            Map<String, Object> baseProps,
            String topic,
            PulsarClientConfig clientConfig,
            MetricRegistry metricRegistry) {
        if (topic == null || topic.isEmpty()) {
            log.warn("Pulsar producer {} skipped: topic is empty", logicalName);
            return null;
        }
        if (baseProps == null || baseProps.isEmpty()) {
            log.warn("Pulsar producer {} skipped: pulsarProducerProps is empty", logicalName);
            return null;
        }
        Map<String, Object> validated = PulsarValidations.validatePulsarProperties(new HashMap<>(baseProps), clientConfig);
        if (validated == null) {
            log.error("Pulsar producer {}: invalid properties", logicalName);
            return null;
        }
        try {
            return new PulsarMessageProducer(validated, topic, metricRegistry);
        } catch (Exception e) {
            log.error("Failed to create PulsarMessageProducer for {}", logicalName, e);
            return null;
        }
    }
}

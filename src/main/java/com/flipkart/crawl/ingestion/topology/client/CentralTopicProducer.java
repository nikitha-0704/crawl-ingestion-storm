package com.flipkart.crawl.ingestion.topology.client;

import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.crawl.ingestion.topology.config.PulsarConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flipkart.pricing.commons.viesti_commons.producer.PulsarMessageProducer;

import java.nio.charset.StandardCharsets;

/**
 * Publishes consolidated enriched JSON to the central topic via viesti {@link PulsarMessageProducer}.
 */
@Slf4j
@RequiredArgsConstructor
public class CentralTopicProducer {

    private final PulsarMessageProducer producer;

    public void publish(EnrichmentTopologyConfig cfg, String key, String enrichedJson) throws Exception {
        PulsarConfig topics = cfg.getPulsarTopics();
        if (topics == null || topics.getCentralEnrichedTopic() == null) {
            log.warn("CENTRAL key={} centralEnrichedTopic not configured; skip publish", key);
            return;
        }
        if (producer == null) {
            log.warn("CENTRAL viesti producer not initialized (check pulsarClientConfig + pulsarProducerProps); skip key={}", key);
            return;
        }
        byte[] payload = enrichedJson == null ? new byte[0] : enrichedJson.getBytes(StandardCharsets.UTF_8);
        producer.send(key, payload);
    }
}

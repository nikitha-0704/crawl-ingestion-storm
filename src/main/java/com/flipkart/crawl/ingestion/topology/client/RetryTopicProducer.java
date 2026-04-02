package com.flipkart.crawl.ingestion.topology.client;

import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.crawl.ingestion.topology.config.PulsarConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flipkart.pricing.commons.viesti_commons.producer.PulsarMessageProducer;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class RetryTopicProducer {

    private final PulsarMessageProducer producer;

    public void publish(EnrichmentTopologyConfig cfg, String key, String payload) throws Exception {
        PulsarConfig topics = cfg.getPulsarTopics();
        if (topics == null || topics.getRetryTopic() == null) {
            log.warn("RETRY key={} retry topic not configured; skip publish", key);
            return;
        }
        if (producer == null) {
            log.warn("RETRY viesti producer not initialized; skip key={}", key);
            return;
        }
        byte[] bytes = payload == null ? new byte[0] : payload.getBytes(StandardCharsets.UTF_8);
        producer.send(key, bytes);
    }
}

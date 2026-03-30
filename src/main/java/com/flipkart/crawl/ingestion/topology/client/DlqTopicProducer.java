package com.flipkart.crawl.ingestion.topology.client;

import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.crawl.ingestion.topology.config.PulsarConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlqTopicProducer {

    public void publish(EnrichmentTopologyConfig cfg, String key, String payload) throws Exception {
        PulsarConfig topics = cfg.getPulsarTopics();
        if (topics == null || topics.getDlqTopic() == null) {
            log.warn("DLQ (stub) key={} dlq topic not configured; skip publish", key);
            return;
        }
        log.info("DLQ (stub) topic={} key={} bytes={}",
                topics.getDlqTopic(), key, payload == null ? 0 : payload.length());
    }
}
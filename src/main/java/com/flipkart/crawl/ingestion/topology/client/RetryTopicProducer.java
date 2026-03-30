package com.flipkart.crawl.ingestion.topology.client;

import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.crawl.ingestion.topology.config.PulsarConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryTopicProducer {

    public void publish(EnrichmentTopologyConfig cfg, String key, String payload) throws Exception {
        PulsarConfig topics = cfg.getPulsarTopics();
        if (topics == null || topics.getRetryTopic() == null) {
            log.warn("RETRY (stub) key={} retry topic not configured; skip publish", key);
            return;
        }
        log.info("RETRY (stub) topic={} key={} bytes={}",
                topics.getRetryTopic(), key, payload == null ? 0 : payload.length());
    }
}
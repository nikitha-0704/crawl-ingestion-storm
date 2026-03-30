package com.flipkart.crawl.ingestion.topology.client;

import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.crawl.ingestion.topology.config.PulsarConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Publishes enriched payload to {@code central-enriched-events}.
 * Stub: logs until Pulsar producer is wired to {@link PulsarConfig#getCentralEnrichedTopic()}.
 */
@Slf4j
public class CentralTopicProducer {

    public void publish(EnrichmentTopologyConfig cfg, String key, String enrichedJson) throws Exception {
        PulsarConfig topics = cfg.getPulsarTopics();
        if (topics == null || topics.getCentralEnrichedTopic() == null) {
            log.warn("CENTRAL (stub) key={} topic not configured; skip publish", key);
            return;
        }
        // TODO: create/reuse Pulsar producer; send to topics.getCentralEnrichedTopic()
        log.info("CENTRAL (stub) topic={} key={} bytes={}",
                topics.getCentralEnrichedTopic(), key,
                enrichedJson == null ? 0 : enrichedJson.length());
    }
}
package com.flipkart.crawl.ingestion.topology.spout;

import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.storm.MessageToValuesMapper;
import org.apache.storm.topology.TopologyBuilder;
import org.flipkart.pricing.commons.viesti_commons.config.DlqConfig;
import org.flipkart.pricing.commons.viesti_commons.connector.PulsarConnector;
import org.flipkart.pricing.commons.viesti_commons.exceptions.PulsarSpoutException;
import org.flipkart.pricing.commons.viesti_commons.spout.PulsarSpoutWithDLQSpillover;

public final class SpoutFactory {

    private SpoutFactory() {}

    public static String createRawSpout(EnrichmentTopologyConfig cfg, TopologyBuilder b, MessageToValuesMapper mapper) {
        return create(cfg.getRawPulsarSpoutConfig().getName(), cfg, b, mapper, cfg.getRawPulsarSpoutConfig());
    }

    public static String createRetrySpout(EnrichmentTopologyConfig cfg, TopologyBuilder b, MessageToValuesMapper mapper) {
        return create(cfg.getRetryPulsarSpoutConfig().getName(), cfg, b, mapper, cfg.getRetryPulsarSpoutConfig());
    }

    private static String create(
            String logicalName,
            EnrichmentTopologyConfig cfg,
            TopologyBuilder b,
            MessageToValuesMapper mapper,
            org.flipkart.pricing.commons.viesti_commons.config.PulsarSpoutConfig spoutCfg) {
        try {
            PulsarConnector connector = new PulsarConnector(cfg.getPulsarClientConfig());
            DlqConfig dlq = cfg.getPulsarDlqConfig();
            PulsarSpoutWithDLQSpillover spout = connector.createSpout(mapper, spoutCfg, dlq);
            b.setSpout(logicalName, spout, spoutCfg.getParallelismHint());
            return logicalName;
        } catch (PulsarClientException e) {
            throw new PulsarSpoutException("Failed to create pulsar spout: " + logicalName, e);
        }
    }
}
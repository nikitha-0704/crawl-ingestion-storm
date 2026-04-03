package com.flipkart.crawl.ingestion.topology;

import com.flipkart.crawl.ingestion.topology.bolt.AsyncEnrichmentBolt;
import com.flipkart.crawl.ingestion.topology.bolt.CentralPublisherBolt;
import com.flipkart.crawl.ingestion.topology.bolt.DLQBolt;
import com.flipkart.crawl.ingestion.topology.bolt.RetryBolt;
import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.crawl.ingestion.topology.config.L2ClientConfig;
import com.flipkart.crawl.ingestion.topology.guice.GuiceEnableHook;
import com.flipkart.crawl.ingestion.topology.spout.RawEventMapper;
import com.flipkart.crawl.ingestion.topology.spout.SpoutFactory;
import com.flipkart.pnp.commons.hook.GuiceInitializerWorkerHook;
import com.flipkart.pnp.commons.storm.utils.AbstractTopologyBuilderWithGuiceWorkerHook;
import lombok.extern.slf4j.Slf4j;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Layer 1: raw + retry Pulsar spouts → enrich (L2) → central topic;
 * failures → retry stream → RetryBolt → DLQ.
 */
@Slf4j
public class EnrichmentTopologyBuilder extends AbstractTopologyBuilderWithGuiceWorkerHook<EnrichmentTopologyConfig> {

    @Override
    public Optional<GuiceInitializerWorkerHook> getGuiceWorkerHook(EnrichmentTopologyConfig config) {
        return Optional.of(new GuiceEnableHook(config));
    }

    @Override
    public TopologyBuilder getStormTopologyBuilder(EnrichmentTopologyConfig config) {
        TopologyBuilder b = new TopologyBuilder();

        String rawSpoutName = SpoutFactory.createRawSpout(config, b, new RawEventMapper());
        String retrySpoutName = SpoutFactory.createRetrySpout(config, b, new RawEventMapper());

        String enrichBoltName = config.getAsyncEnrichmentBoltConfig().getName();
        AsyncEnrichmentBolt enrichBolt = new AsyncEnrichmentBolt();
        L2ClientConfig l2 = config.getL2ClientConfig();
        if (l2 != null && l2.getMicroBatchWindowCount() > 0) {
            enrichBolt.withTumblingWindow(new BaseWindowedBolt.Count(l2.getMicroBatchWindowCount()));
        } else {
            int sec = l2 != null && l2.getMicroBatchWindowSeconds() > 0
                    ? l2.getMicroBatchWindowSeconds()
                    : 30;
            enrichBolt.withTumblingWindow(new BaseWindowedBolt.Duration(sec, TimeUnit.SECONDS));
        }
        b.setBolt(enrichBoltName, enrichBolt, config.getAsyncEnrichmentBoltConfig().getParallelismHint())
                .fieldsGrouping(rawSpoutName, new Fields(Constants.KEY))
                .fieldsGrouping(retrySpoutName, new Fields(Constants.KEY));

        String centralBoltName = config.getCentralPublisherBoltConfig().getName();
        b.setBolt(centralBoltName, new CentralPublisherBolt(), config.getCentralPublisherBoltConfig().getParallelismHint())
                .fieldsGrouping(enrichBoltName, new Fields(Constants.KEY));

        String retryBoltName = config.getRetryBoltConfig().getName();
        b.setBolt(retryBoltName, new RetryBolt(), config.getRetryBoltConfig().getParallelismHint())
                .fieldsGrouping(enrichBoltName, Constants.RETRY_STREAM, new Fields(Constants.KEY));

        String dlqBoltName = config.getDlqBoltConfig().getName();
        b.setBolt(dlqBoltName, new DLQBolt(), config.getDlqBoltConfig().getParallelismHint())
                .fieldsGrouping(retryBoltName, Constants.DLQ_STREAM, new Fields(Constants.KEY));

        log.info("Enrichment topology graph wired: raw={}, retry={}, enrich={}, central={}, retryBolt={}, dlq={}",
                rawSpoutName, retrySpoutName, enrichBoltName, centralBoltName, retryBoltName, dlqBoltName);

        return b;
    }
}
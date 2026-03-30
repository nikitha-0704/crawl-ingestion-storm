package com.flipkart.crawl.ingestion.topology.bolt;

import com.flipkart.crawl.ingestion.topology.Constants;
import com.flipkart.crawl.ingestion.topology.client.L2Client;
import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.crawl.ingestion.topology.util.RetryUtil;
import com.flipkart.pnp.commons.init.StormGuiceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.Map;

@Slf4j
public class AsyncEnrichmentBolt extends BaseRichBolt {

    private transient OutputCollector collector;
    private transient L2Client l2Client;
    private transient EnrichmentTopologyConfig cfg;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.l2Client = StormGuiceContext.getInstance(L2Client.class);
        this.cfg = StormGuiceContext.getInstance(EnrichmentTopologyConfig.class);
    }

    @Override
    public void execute(Tuple input) {
        String key = input.getStringByField(Constants.KEY);
        String rawJson = input.getStringByField(Constants.RAW_EVENT);
        String jsonForL2 = RetryUtil.extractRawEventJson(rawJson);
        try {
            String enrichedJson = l2Client.enrich(jsonForL2, cfg.getL2ClientConfig());
            // Pass inner crawl JSON for central bolt to merge raw + L2 + routing tags.
            collector.emit(input, new Values(key, jsonForL2, enrichedJson));
            collector.ack(input);
        } catch (Exception e) {
            log.error("L2 enrich failed for key={}", key, e);
            // Preserve tuple payload (plain raw or envelope) so RetryBolt can bump attempt.
            collector.emit(Constants.RETRY_STREAM, input, new Values(key, rawJson));
            collector.ack(input);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(Constants.KEY, Constants.RAW_EVENT, Constants.ENRICHED_EVENT));
        declarer.declareStream(Constants.RETRY_STREAM, new Fields(Constants.KEY, Constants.RAW_EVENT));
    }
}
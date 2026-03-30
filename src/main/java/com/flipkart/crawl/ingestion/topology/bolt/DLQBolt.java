package com.flipkart.crawl.ingestion.topology.bolt;

import com.flipkart.crawl.ingestion.topology.Constants;
import com.flipkart.crawl.ingestion.topology.client.DlqTopicProducer;
import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.pnp.commons.init.StormGuiceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;

import java.util.Map;

@Slf4j
public class DLQBolt extends BaseRichBolt {

    private transient OutputCollector collector;
    private transient DlqTopicProducer dlq;
    private transient EnrichmentTopologyConfig cfg;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.dlq = StormGuiceContext.getInstance(DlqTopicProducer.class);
        this.cfg = StormGuiceContext.getInstance(EnrichmentTopologyConfig.class);
    }

    @Override
    public void execute(Tuple input) {
        String key = input.getStringByField(Constants.KEY);
        String raw = input.getStringByField(Constants.RAW_EVENT);
        try {
            dlq.publish(cfg, key, raw);
            collector.ack(input);
        } catch (Exception e) {
            log.error("DLQ publish failed key={}", key, e);
            collector.fail(input);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }
}
package com.flipkart.crawl.ingestion.topology.bolt;

import com.flipkart.crawl.ingestion.topology.Constants;
import com.flipkart.crawl.ingestion.topology.client.RetryTopicProducer;
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
public class RetryBolt extends BaseRichBolt {

    private transient OutputCollector collector;
    private transient RetryTopicProducer retryProducer;
    private transient EnrichmentTopologyConfig cfg;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.retryProducer = StormGuiceContext.getInstance(RetryTopicProducer.class);
        this.cfg = StormGuiceContext.getInstance(EnrichmentTopologyConfig.class);
    }

    @Override
    public void execute(Tuple input) {
        String key = input.getStringByField(Constants.KEY);
        String raw = input.getStringByField(Constants.RAW_EVENT);
        int max = cfg.getRetryConfig() != null && cfg.getRetryConfig().getMaxAttempts() > 0
                ? cfg.getRetryConfig().getMaxAttempts()
                : 3;
        try {
            int current = RetryUtil.currentAttempt(raw);
            String innerRaw = RetryUtil.extractRawEventJson(raw);
            int nextAttempt = current + 1;
            if (nextAttempt >= max) {
                collector.emit(Constants.DLQ_STREAM, input, new Values(key, raw, nextAttempt));
                collector.ack(input);
                return;
            }
            String payload = RetryUtil.wrapWithAttempt(innerRaw, nextAttempt);
            retryProducer.publish(cfg, key, payload);
            collector.ack(input);
        } catch (Exception e) {
            log.error("Retry publish failed key={}", key, e);
            collector.fail(input);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(Constants.DLQ_STREAM, new Fields(Constants.KEY, Constants.RAW_EVENT, "attempt"));
    }
}
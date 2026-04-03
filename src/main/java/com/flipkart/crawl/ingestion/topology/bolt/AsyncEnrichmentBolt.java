package com.flipkart.crawl.ingestion.topology.bolt;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.crawl.ingestion.topology.Constants;
import com.flipkart.crawl.ingestion.topology.client.L2Client;
import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.crawl.ingestion.topology.metrics.MetricsNames;
import com.flipkart.crawl.ingestion.topology.metrics.TopologyMetrics;
import com.flipkart.crawl.ingestion.topology.util.L2ResponseSlicer;
import com.flipkart.crawl.ingestion.topology.util.RetryUtil;
import com.flipkart.crawl.ingestion.topology.util.ValidationUtil;
import com.flipkart.pnp.commons.init.StormGuiceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Micro-batches tuples per {@code KEY} (fieldsGrouping): one L2 HTTP call per window chunk, up to {@code maxBatchSize}.
 */
@Slf4j
public class AsyncEnrichmentBolt extends BaseWindowedBolt {

    private transient OutputCollector collector;
    private transient L2Client l2Client;
    private transient EnrichmentTopologyConfig cfg;
    private transient TopologyMetrics metrics;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.l2Client = StormGuiceContext.getInstance(L2Client.class);
        this.cfg = StormGuiceContext.getInstance(EnrichmentTopologyConfig.class);
        String prefix = context.getThisComponentId();
        this.metrics = new TopologyMetrics(context, prefix,
                MetricsNames.ENRICH_SUCCESS,
                MetricsNames.ENRICH_FAILURE,
                MetricsNames.ENRICH_RETRY_EMIT,
                MetricsNames.VALIDATION_REJECT);
    }

    @Override
    public void execute(TupleWindow inputWindow) {
        List<Tuple> tuples = inputWindow.get();
        if (tuples.isEmpty()) {
            return;
        }
        int maxBatch = cfg.getL2ClientConfig().getMaxBatchSize();
        if (maxBatch <= 0) {
            maxBatch = 30;
        }
        for (int start = 0; start < tuples.size(); start += maxBatch) {
            int end = Math.min(start + maxBatch, tuples.size());
            processSubBatch(tuples.subList(start, end));
        }
    }

    private void processSubBatch(List<Tuple> slice) {
        List<Tuple> validTuples = new ArrayList<>(slice.size());
        List<String> validJsons = new ArrayList<>(slice.size());
        for (Tuple input : slice) {
            String key = input.getStringByField(Constants.KEY);
            String rawJson = input.getStringByField(Constants.RAW_EVENT);
            String jsonForL2 = RetryUtil.extractRawEventJson(rawJson);
            String validationError = ValidationUtil.validateRawEventJson(jsonForL2);
            if (validationError != null) {
                log.warn("Validation failed key={}: {}", key, validationError);
                metrics.inc(MetricsNames.VALIDATION_REJECT);
                metrics.inc(MetricsNames.ENRICH_RETRY_EMIT);
                collector.emit(Constants.RETRY_STREAM, input, new Values(key, rawJson));
                collector.ack(input);
                continue;
            }
            validTuples.add(input);
            validJsons.add(jsonForL2);
        }
        if (validTuples.isEmpty()) {
            return;
        }
        try {
            String l2Full = l2Client.enrichBatch(validJsons, cfg.getL2ClientConfig());
            JsonNode enrichedArray = L2ResponseSlicer.enrichedEventsArray(l2Full);
            if (enrichedArray != null && enrichedArray.size() != validTuples.size()) {
                log.error("L2 enriched_events size {} != batch size {}; sending batch to retry",
                        enrichedArray.size(), validTuples.size());
                metrics.inc(MetricsNames.ENRICH_FAILURE);
                metrics.inc(MetricsNames.ENRICH_RETRY_EMIT, validTuples.size());
                emitRetryForBatch(validTuples);
                return;
            }
            metrics.inc(MetricsNames.ENRICH_SUCCESS, validTuples.size());
            for (int i = 0; i < validTuples.size(); i++) {
                Tuple input = validTuples.get(i);
                String key = input.getStringByField(Constants.KEY);
                String jsonForL2 = validJsons.get(i);
                String sliced = L2ResponseSlicer.sliceForTuple(l2Full, i, enrichedArray);
                collector.emit(input, new Values(key, jsonForL2, sliced));
                collector.ack(input);
            }
        } catch (Exception e) {
            log.error("L2 enrich batch failed size={}", validTuples.size(), e);
            metrics.inc(MetricsNames.ENRICH_FAILURE);
            metrics.inc(MetricsNames.ENRICH_RETRY_EMIT, validTuples.size());
            emitRetryForBatch(validTuples);
        }
    }

    private void emitRetryForBatch(List<Tuple> validTuples) {
        for (Tuple input : validTuples) {
            String key = input.getStringByField(Constants.KEY);
            String rawJson = input.getStringByField(Constants.RAW_EVENT);
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

package com.flipkart.crawl.ingestion.topology.metrics;

import org.apache.storm.metric.api.CountMetric;
import org.apache.storm.task.TopologyContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-bolt counters; each task gets its own metrics instance.
 */
public final class TopologyMetrics {

    private final Map<String, CountMetric> metrics = new HashMap<>();

    public TopologyMetrics(TopologyContext ctx, String componentPrefix) {
        register(ctx, componentPrefix, MetricsNames.ENRICH_SUCCESS);
        register(ctx, componentPrefix, MetricsNames.ENRICH_FAILURE);
        register(ctx, componentPrefix, MetricsNames.ENRICH_RETRY_EMIT);
        register(ctx, componentPrefix, MetricsNames.ENRICH_DLQ_EMIT);
        register(ctx, componentPrefix, MetricsNames.CENTRAL_PUBLISH_SUCCESS);
        register(ctx, componentPrefix, MetricsNames.CENTRAL_PUBLISH_FAILURE);
        register(ctx, componentPrefix, MetricsNames.RETRY_PUBLISH_SUCCESS);
        register(ctx, componentPrefix, MetricsNames.RETRY_PUBLISH_FAILURE);
        register(ctx, componentPrefix, MetricsNames.DLQ_PUBLISH_SUCCESS);
        register(ctx, componentPrefix, MetricsNames.DLQ_PUBLISH_FAILURE);
        register(ctx, componentPrefix, MetricsNames.VALIDATION_REJECT);
    }

    private void register(TopologyContext ctx, String prefix, String name) {
        CountMetric m = new CountMetric();
        metrics.put(name, m);
        ctx.registerMetric(prefix + "." + name, m, 60);
    }

    public void inc(String name) {
        CountMetric m = metrics.get(name);
        if (m != null) {
            m.incr();
        }
    }

    public void inc(String name, long delta) {
        CountMetric m = metrics.get(name);
        if (m != null) {
            m.incrBy(delta);
        }
    }
}
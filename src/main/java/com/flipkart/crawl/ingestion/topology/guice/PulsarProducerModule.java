package com.flipkart.crawl.ingestion.topology.guice;

import com.codahale.metrics.MetricRegistry;
import com.flipkart.crawl.ingestion.topology.Constants;
import com.flipkart.crawl.ingestion.topology.client.CentralTopicProducer;
import com.flipkart.crawl.ingestion.topology.client.DlqTopicProducer;
import com.flipkart.crawl.ingestion.topology.client.RetryTopicProducer;
import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.flipkart.pricing.commons.viesti_commons.producer.PulsarMessageProducer;

import java.util.Map;

/**
 * Wires viesti {@link PulsarMessageProducer} beans (same approach as enriched-order-consumer {@code ConfigurationModule}).
 */
public class PulsarProducerModule extends AbstractModule {

    private final EnrichmentTopologyConfig cfg;

    public PulsarProducerModule(EnrichmentTopologyConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        // @Provides methods below
    }

    @Provides
    @Singleton
    MetricRegistry pulsarMetricRegistry() {
        return new MetricRegistry();
    }

    @Provides
    @Singleton
    @Named(Constants.PULSAR_PRODUCERS_MAP)
    Map<String, PulsarMessageProducer> pulsarProducersMap(MetricRegistry pulsarMetricRegistry) {
        return PulsarProducerFactory.build(cfg, pulsarMetricRegistry);
    }

    @Provides
    @Singleton
    CentralTopicProducer centralTopicProducer(@Named(Constants.PULSAR_PRODUCERS_MAP) Map<String, PulsarMessageProducer> map) {
        return new CentralTopicProducer(map.get(Constants.CENTRAL_PULSAR_PRODUCER));
    }

    @Provides
    @Singleton
    RetryTopicProducer retryTopicProducer(@Named(Constants.PULSAR_PRODUCERS_MAP) Map<String, PulsarMessageProducer> map) {
        return new RetryTopicProducer(map.get(Constants.RETRY_PULSAR_PRODUCER));
    }

    @Provides
    @Singleton
    DlqTopicProducer dlqTopicProducer(@Named(Constants.PULSAR_PRODUCERS_MAP) Map<String, PulsarMessageProducer> map) {
        return new DlqTopicProducer(map.get(Constants.DLQ_PULSAR_PRODUCER));
    }
}

package com.flipkart.crawl.ingestion.topology.guice;

import com.flipkart.crawl.ingestion.topology.client.*;
import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class TopologyModule extends AbstractModule {

    private final EnrichmentTopologyConfig cfg;

    public TopologyModule(EnrichmentTopologyConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        bind(EnrichmentTopologyConfig.class).toInstance(cfg);
        bind(L2Client.class).to(L2HttpClient.class).in(Singleton.class);
        bind(CentralTopicProducer.class).in(Singleton.class);
        bind(RetryTopicProducer.class).in(Singleton.class);
        bind(DlqTopicProducer.class).in(Singleton.class);
    }
}
package com.flipkart.crawl.ingestion.topology.guice;

import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.pnp.commons.hook.GuiceInitializerWorkerHook;
import com.google.common.collect.Sets;
import com.google.inject.Module;
import org.apache.storm.task.WorkerTopologyContext;

import java.io.Serializable;
import java.util.Set;

public class GuiceEnableHook extends GuiceInitializerWorkerHook<EnrichmentTopologyConfig> implements Serializable {

    public GuiceEnableHook(EnrichmentTopologyConfig topologyConfig) {
        super(topologyConfig);
    }

    @Override
    protected Set<Module> getInjectableModules(WorkerTopologyContext workerTopologyContext) {
        return Sets.newHashSet(new TopologyModule(getTopologyConfig()));
    }

    @Override
    protected void gracefullyShutdownAndReleaseResources() {
        // close shared clients if you register them in TopologyModule
    }
}
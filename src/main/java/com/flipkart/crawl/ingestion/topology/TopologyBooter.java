package com.flipkart.crawl.ingestion.topology;

import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.pnp.commons.storm.config.TopologyConfig;
import com.flipkart.pnp.commons.storm.utils.AbstractMultiTopologyBooter;
import com.flipkart.pnp.commons.storm.utils.ConfigParserUtil;
import com.flipkart.pnp.commons.storm.utils.TopologyBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Entry point for submitting the crawl ingestion enrichment topology.
 * Usage: TopologyBooter &lt;configFilePath&gt; &lt;topologyName&gt;
 */
@Slf4j
public class TopologyBooter extends AbstractMultiTopologyBooter {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException("Usage: TopologyBooter <configFilePath> <topologyName>");
        }
        String[] topologyArgs = new String[]{
                args[0],
                args[1],
                EnrichmentTopologyConfig.class.getName(),
                EnrichmentTopologyBuilder.class.getName()
        };
        new TopologyBooter().run(topologyArgs);
        log.debug("Submitted topology: {}", args[1]);
    }

    @Override
    protected Map<TopologyConfig, TopologyBuilder> getTopologyBuilderMap(String configFilePath) throws Exception {
        EnrichmentTopologyConfig config = ConfigParserUtil.parse(configFilePath, EnrichmentTopologyConfig.class);
        Map<TopologyConfig, TopologyBuilder> builders = new HashMap<>();
        builders.put(config, new EnrichmentTopologyBuilder());
        return builders;
    }
}
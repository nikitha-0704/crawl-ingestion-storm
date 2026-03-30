package com.flipkart.crawl.ingestion.topology.config;

import com.flipkart.pnp.commons.storm.config.BaseStormUnitConfig;
import com.flipkart.pnp.commons.storm.config.TopologyConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.flipkart.pricing.commons.viesti_commons.config.DlqConfig;
import org.flipkart.pricing.commons.viesti_commons.config.PulsarClientConfig;
import org.flipkart.pricing.commons.viesti_commons.config.PulsarSpoutConfig;

import java.io.Serializable;

/**
 * Layer-1 topology YAML binding: Pulsar in, L2 client settings, bolt units, retry policy.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnrichmentTopologyConfig extends TopologyConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Shared Pulsar client (viesti). */
    private PulsarClientConfig pulsarClientConfig;

    /** Consumer on raw-crawl-events. */
    private PulsarSpoutConfig rawPulsarSpoutConfig;

    /** Consumer on retry topic (same payload shape as raw). */
    private PulsarSpoutConfig retryPulsarSpoutConfig;

    /** DLQ policy for Pulsar spouts (viesti). */
    private DlqConfig pulsarDlqConfig;

    /** Call L2 enrichment service from Storm. */
    private L2ClientConfig l2ClientConfig;

    /** Max attempts before DLQ (used by RetryBolt). */
    private RetryConfig retryConfig;

    private BaseStormUnitConfig asyncEnrichmentBoltConfig;
    private BaseStormUnitConfig centralPublisherBoltConfig;
    private BaseStormUnitConfig retryBoltConfig;
    private BaseStormUnitConfig dlqBoltConfig;
    private PulsarConfig pulsarTopics;
    private TopologyRuntimeConfig runtime;
}
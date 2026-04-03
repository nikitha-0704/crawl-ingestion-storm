package com.flipkart.crawl.ingestion.topology.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flipkart.pnp.commons.storm.config.BaseStormUnitConfig;
import com.flipkart.pnp.commons.storm.config.TopologyConfig;
import lombok.Getter;
import org.flipkart.pricing.commons.viesti_commons.config.DlqConfig;
import org.flipkart.pricing.commons.viesti_commons.config.PulsarClientConfig;
import org.flipkart.pricing.commons.viesti_commons.config.PulsarSpoutConfig;

import java.io.Serializable;
import java.util.Map;

/**
 * Layer-1 topology YAML binding: Pulsar in, L2 client settings, bolt units, retry policy.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrichmentTopologyConfig extends TopologyConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Shared Pulsar client (viesti). */
    private final PulsarClientConfig pulsarClientConfig;

    /**
     * Viesti producer properties (bootstrap.servers, serializers, pulsar.authentication.class, …).
     * Auth params are merged from {@link #pulsarClientConfig} via {@link com.flipkart.crawl.ingestion.topology.util.PulsarValidations}.
     */
    private final Map<String, Object> pulsarProducerProps;

    /** Consumer on raw-crawl-events. */
    private final PulsarSpoutConfig rawPulsarSpoutConfig;

    /** Consumer on retry topic (same payload shape as raw). */
    private final PulsarSpoutConfig retryPulsarSpoutConfig;

    /** DLQ policy for Pulsar spouts (viesti). */
    private final DlqConfig pulsarDlqConfig;

    /** Call L2 enrichment service from Storm. */
    private final L2ClientConfig l2ClientConfig;

    /** Max attempts before DLQ (used by RetryBolt). */
    private final RetryConfig retryConfig;

    private final BaseStormUnitConfig asyncEnrichmentBoltConfig;
    private final BaseStormUnitConfig centralPublisherBoltConfig;
    private final BaseStormUnitConfig retryBoltConfig;
    private final BaseStormUnitConfig dlqBoltConfig;
    private final PulsarConfig pulsarTopics;
    private final TopologyRuntimeConfig runtime;

    /** Optional classifier config; when null or disabled, routing uses no pipeline list from DB. */
    private final PipelineRoutingConfig pipelineRouting;

    protected EnrichmentTopologyConfig(
            @JsonProperty("name") String name,
            @JsonProperty("workers") Integer workers,
            @JsonProperty("ackers") Integer ackers,
            @JsonProperty("debug") Boolean debug,
            @JsonProperty("maxSpoutPending") Integer maxSpoutPending,
            @JsonProperty("topologyTimeoutInSecs") Integer topologyTimeoutInSecs,
            @JsonProperty("topologyStatsSampleRate") Double topologyStatsSampleRate,
            @JsonProperty("additionalStormProperties") Map<String, Object> additionalStormProperties,
            @JsonProperty("pulsarClientConfig") PulsarClientConfig pulsarClientConfig,
            @JsonProperty("pulsarProducerProps") Map<String, Object> pulsarProducerProps,
            @JsonProperty("rawPulsarSpoutConfig") PulsarSpoutConfig rawPulsarSpoutConfig,
            @JsonProperty("retryPulsarSpoutConfig") PulsarSpoutConfig retryPulsarSpoutConfig,
            @JsonProperty("pulsarDlqConfig") DlqConfig pulsarDlqConfig,
            @JsonProperty("l2ClientConfig") L2ClientConfig l2ClientConfig,
            @JsonProperty("retryConfig") RetryConfig retryConfig,
            @JsonProperty("asyncEnrichmentBoltConfig") BaseStormUnitConfig asyncEnrichmentBoltConfig,
            @JsonProperty("centralPublisherBoltConfig") BaseStormUnitConfig centralPublisherBoltConfig,
            @JsonProperty("retryBoltConfig") BaseStormUnitConfig retryBoltConfig,
            @JsonProperty("dlqBoltConfig") BaseStormUnitConfig dlqBoltConfig,
            @JsonProperty("pulsarTopics") PulsarConfig pulsarTopics,
            @JsonProperty("runtime") TopologyRuntimeConfig runtime,
            @JsonProperty("pipelineRouting") PipelineRoutingConfig pipelineRouting) {
        super(name, workers, ackers, debug, maxSpoutPending, topologyTimeoutInSecs, topologyStatsSampleRate,
                additionalStormProperties);
        this.pulsarClientConfig = pulsarClientConfig;
        this.pulsarProducerProps = pulsarProducerProps;
        this.rawPulsarSpoutConfig = rawPulsarSpoutConfig;
        this.retryPulsarSpoutConfig = retryPulsarSpoutConfig;
        this.pulsarDlqConfig = pulsarDlqConfig;
        this.l2ClientConfig = l2ClientConfig;
        this.retryConfig = retryConfig;
        this.asyncEnrichmentBoltConfig = asyncEnrichmentBoltConfig;
        this.centralPublisherBoltConfig = centralPublisherBoltConfig;
        this.retryBoltConfig = retryBoltConfig;
        this.dlqBoltConfig = dlqBoltConfig;
        this.pulsarTopics = pulsarTopics;
        this.runtime = runtime;
        this.pipelineRouting = pipelineRouting;
    }
}

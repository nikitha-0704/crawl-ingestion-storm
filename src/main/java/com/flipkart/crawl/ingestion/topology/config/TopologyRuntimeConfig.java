package com.flipkart.crawl.ingestion.topology.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class TopologyRuntimeConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Storm workers (if not set on parent TopologyConfig). */
    private Integer workers;

    /** Storm ackers. */
    private Integer ackers;

    private Boolean debug;

    /** Topology-level message timeout (seconds), if your platform uses it. */
    private Integer topologyTimeoutInSecs;

    /** Backpressure / max unacked from spout. */
    private Integer maxSpoutPending;

    /** Stats sample rate for Storm metrics (0.0–1.0) if used. */
    private Double topologyStatsSampleRate;
}
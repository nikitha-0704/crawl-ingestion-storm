package com.flipkart.crawl.ingestion.topology.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * Optional MySQL-backed pipeline classifier. When disabled, {@link com.flipkart.crawl.ingestion.topology.routing.PipelineRoutingRegistry#NOOP} behaviour applies.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PipelineRoutingConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean enabled = false;

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    /** Full refresh interval. */
    private int refreshIntervalMinutes = 60;

    /**
     * Must return columns {@code site_id}, {@code competitor_id}, {@code pipeline_name} (one row per pipeline tag).
     */
    private String sql = "SELECT site_id, competitor_id, pipeline_name FROM crawl_site_competitor_pipeline";
}

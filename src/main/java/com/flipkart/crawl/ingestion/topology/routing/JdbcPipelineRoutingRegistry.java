package com.flipkart.crawl.ingestion.topology.routing;

import com.flipkart.crawl.ingestion.topology.config.EnrichmentTopologyConfig;
import com.flipkart.crawl.ingestion.topology.config.PipelineRoutingConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads (site_id, competitor_id) → pipeline list from MySQL on a fixed schedule; reads are in-memory only.
 */
@Slf4j
@Singleton
public class JdbcPipelineRoutingRegistry implements PipelineRoutingRegistry {

    private final PipelineRoutingConfig cfg;
    private final AtomicReference<Map<String, List<String>>> routes =
            new AtomicReference<>(Collections.emptyMap());
    private final ScheduledExecutorService scheduler;

    @Inject
    public JdbcPipelineRoutingRegistry(EnrichmentTopologyConfig topologyConfig) {
        this.cfg = topologyConfig.getPipelineRouting();
        if (cfg != null && cfg.isEnabled()) {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "crawl-pipeline-routing-refresh");
                t.setDaemon(true);
                return t;
            });
            safeRefresh();
            int mins = cfg.getRefreshIntervalMinutes() > 0 ? cfg.getRefreshIntervalMinutes() : 60;
            scheduler.scheduleAtFixedRate(this::safeRefresh, mins, mins, TimeUnit.MINUTES);
        } else {
            this.scheduler = null;
        }
    }

    private void safeRefresh() {
        try {
            refresh();
        } catch (Exception e) {
            log.warn("Pipeline routing refresh failed; keeping previous snapshot", e);
        }
    }

    void refresh() throws Exception {
        if (cfg == null || !cfg.isEnabled()) {
            return;
        }
        if (cfg.getJdbcUrl() == null || cfg.getJdbcUrl().isEmpty()) {
            log.warn("pipelineRouting.enabled but jdbcUrl is empty; skipping load");
            return;
        }
        Map<String, List<String>> next = loadAll();
        routes.set(Collections.unmodifiableMap(next));
        log.info("Pipeline routing map loaded: {} keys", next.size());
    }

    private Map<String, List<String>> loadAll() throws Exception {
        Map<String, List<String>> accum = new LinkedHashMap<>();
        try (Connection c = DriverManager.getConnection(cfg.getJdbcUrl(), nullToEmpty(cfg.getJdbcUser()),
                nullToEmpty(cfg.getJdbcPassword()));
             PreparedStatement ps = c.prepareStatement(cfg.getSql());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int site = rs.getInt("site_id");
                if (rs.wasNull()) {
                    continue;
                }
                int comp = rs.getInt("competitor_id");
                if (rs.wasNull()) {
                    continue;
                }
                String pipeline = rs.getString("pipeline_name");
                if (pipeline == null || pipeline.isEmpty()) {
                    continue;
                }
                String k = key(site, comp);
                accum.computeIfAbsent(k, x -> new ArrayList<>()).add(pipeline);
            }
        }
        Map<String, List<String>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : accum.entrySet()) {
            frozen.put(e.getKey(), Collections.unmodifiableList(new ArrayList<>(e.getValue())));
        }
        return frozen;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String key(int siteId, int competitorId) {
        return siteId + "_" + competitorId;
    }

    @Override
    public List<String> pipelinesFor(Integer siteId, Integer competitorId) {
        if (siteId == null || competitorId == null) {
            return Collections.emptyList();
        }
        List<String> p = routes.get().get(key(siteId, competitorId));
        return p != null ? p : Collections.emptyList();
    }
}

package com.flipkart.crawl.ingestion.topology.client;

import com.flipkart.crawl.ingestion.topology.config.L2ClientConfig;
import com.flipkart.crawl.ingestion.topology.model.EnrichIdsRequest;
import com.flipkart.crawl.ingestion.topology.model.IdRef;
import com.flipkart.crawl.ingestion.topology.util.JsonUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class L2HttpClient implements L2Client {

    @Override
    public String enrichBatch(List<String> rawEventJsons, L2ClientConfig cfg) throws Exception {
        List<IdRef> refs = new ArrayList<>(rawEventJsons.size());
        for (String json : rawEventJsons) {
            refs.add(IdRef.fromCrawlJson(json));
        }
        EnrichIdsRequest req = new EnrichIdsRequest();
        req.setEvents(refs);

        String url = cfg.getBaseUrl() + cfg.getEnrichPath();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(cfg.getConnectTimeoutMs());
        conn.setReadTimeout(cfg.getReadTimeoutMs());
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        byte[] body = JsonUtil.MAPPER.writeValueAsBytes(req);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String resp = readAll(stream);
        if (code >= 200 && code < 300) {
            return resp;
        }
        throw new IllegalStateException("L2 HTTP " + code + ": " + resp);
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b)) != -1) {
            buf.write(b, 0, n);
        }
        return buf.toString(StandardCharsets.UTF_8.name());
    }
}

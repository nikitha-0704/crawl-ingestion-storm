package com.flipkart.crawl.ingestion.topology.spout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.crawl.ingestion.topology.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.storm.MessageToValuesMapper;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@Slf4j
public class RawEventMapper implements MessageToValuesMapper, Serializable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Values toValues(Message<byte[]> msg) {
        String json = new String(msg.getData(), StandardCharsets.UTF_8);
        String key = extractKey(json, msg);
        return new Values(key, json);
    }

    private String extractKey(String json, Message<byte[]> msg) {
        try {
            JsonNode n = MAPPER.readTree(json);
            if (n.hasNonNull("product_id")) {
                return String.valueOf(n.get("product_id").asLong());
            }
        } catch (Exception e) {
            log.warn("Could not parse product_id from payload, falling back to message key / id: {}", e.toString());
        }
        if (msg.hasKey()) {
            byte[] keyBytes = msg.getKeyBytes();
            if (keyBytes != null && keyBytes.length > 0) {
                return new String(keyBytes, StandardCharsets.UTF_8);
            }
        }
        return String.valueOf(msg.getMessageId());
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(Constants.KEY, Constants.RAW_EVENT));
    }
}
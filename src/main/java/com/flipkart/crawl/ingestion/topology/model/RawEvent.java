package com.flipkart.crawl.ingestion.topology.model;

import lombok.Data;

import java.util.List;

@Data
public class RawEvent {
    private Integer schema_version;
    private Long product_id;
    private Integer site_id;
    private Integer competitor_id;
    private String sku;
    private Double price;
    private Integer avail_id;
    private Long when_seen;
    private String url;
    private String category;
    private List<String> bank_offers;
}
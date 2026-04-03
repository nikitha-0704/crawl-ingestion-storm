# crawl-ingestion-storm

Layer **1** ingestion topology on **Apache Storm**: consume crawl events from **Pulsar** (raw + retry topics), call **Layer 2** enrichment over **HTTP** with **identifier-only** requests, merge **routing tags** and L2 data into a **consolidated payload**, publish to a **central** Pulsar topic, and route failures through a **retry** topic to **DLQ** after `maxAttempts`.

## Documentation

| Doc | Description |
|-----|-------------|
| [docs/lld-layer1.md](docs/lld-layer1.md) | Low-level design: components, tuples, streams, config |
| [docs/sequence-flow.md](docs/sequence-flow.md) | Sequence diagrams (Mermaid): happy path, retry, DLQ |

## Requirements

- **JDK 8+** (project targets Java 8)
- **Maven 3.6+**
- Internal **Flipkart Artifactory** access for `storm-commons`, Pulsar/viesti libraries (see `pom.xml`)

## Build

```bash
mvn clean compile -DskipTests
```

## Tests

```bash
mvn test
```

Requires the same dependencies as compile (internal Artifactory). Unit tests cover `RetryUtil`, `ValidationUtil`, `RoutingTagsBuilder`, and `ConsolidatedPayloadBuilder` (no Storm cluster).

## Runtime behavior

- **Micro-batching (L2):** `AsyncEnrichmentBolt` extends Storm’s `BaseWindowedBolt` with a tumbling window (default **count 30** via `l2ClientConfig.microBatchWindowCount`, or set to `0` and use `microBatchWindowSeconds` for a time window). Within each window it calls `L2Client.enrichBatch` with up to `maxBatchSize` identifiers per HTTP request and fans out per-tuple emits using the returned `enriched_events` order.
- **Classifier:** Optional `pipelineRouting` loads `(site_id, competitor_id) → pipeline names` from MySQL on a schedule; `RoutingTagsBuilder` adds a `pipelines` array on the consolidated payload when a mapping exists (`enabled: false` keeps prior behaviour).
- **Validation:** Rejects inner crawl JSON without a valid `product_id` (same path as L2 failure: retry stream → `RetryBolt` / DLQ policy).
- **Metrics:** Bolts register Storm `CountMetric`s via `TopologyMetrics` (success/failure/retry/DLQ/validation counters per component).

## GCP: fk-3p-storm GCS buckets (storm.yaml / cluster + worker XML)

Instance startup scripts (`scripts/gcp-instance-template-nimbus.sh`, `gcp-instance-template-supervisor.sh`) default to:

| Bucket | Role |
|--------|------|
| `ci-storm-nimbus-stage` | `STORM_YAML` + `CLUSTER_XML` |
| `ci-storm-supervisor-stage` | `WORKER_XML` |

Example project: **upst-explore-9988** — [nimbus bucket](https://console.cloud.google.com/storage/browser/ci-storm-nimbus-stage?project=upst-explore-9988), [supervisor bucket](https://console.cloud.google.com/storage/browser/ci-storm-supervisor-stage?project=upst-explore-9988). Upload Storm config objects per platform; grant VM service accounts read access.

## Debian package & confd (same idea as `pricing-commons/batching-topology`)

| Path | Purpose |
|------|---------|
| `debian/` | `control`, `postinst`, confd `crawl-enrichment-topology.toml` + `.tmpl` → `/etc/fk-3p-storm/conf/crawl-enrichment-topology.yaml` |
| `config/crawlEnrichmentTopology.yaml` | Sample topology YAML (mirror of `src/main/resources/enrichment-topology.yaml`) |
| `scripts/make-deb.sh` | After `mvn package`, builds `crawl-ingestion-storm.deb` with shaded jar under `/usr/share/crawl-ingestion-storm/lib/` |

Confd key prefix: `crawl-enrichment-topology-{env}` (see `debian/etc/confd/conf.d/crawl-enrichment-topology.toml`). Wrapper script: `crawl-ingestion-storm` (`run-class` / `start-all` per `/etc/default/crawl-ingestion-storm`).

```bash
mvn clean package
./scripts/make-deb.sh stage
```

## Run (submit topology)

Entry point:

`com.flipkart.crawl.ingestion.topology.TopologyBooter`

Arguments: `<configFilePath> <topologyName>`

Example:

```bash
java -cp target/crawl-ingestion-storm-*.jar com.flipkart.crawl.ingestion.topology.TopologyBooter \
  /path/to/enrichment-topology.yaml CrawlEnrichmentTopology
```

Fill `src/main/resources/enrichment-topology.yaml` (or an external file) with real **viesti/Pulsar** settings for your environment. **Preprod** tenant/namespace and producer topic names are already set in that file; complete `pulsarClientConfig` and spout configs per viesti.

### Pulsar topics (preprod — `ci-preprod` / `ci-crawl-enricher`)

| Role | Full topic name |
|------|-----------------|
| Raw crawl (spouts) | `persistent://ci-preprod/ci-crawl-enricher/ci-crawl-events-raw-preprod` |
| Enriched (central producer) | `persistent://ci-preprod/ci-crawl-enricher/ci-enriched-events-preprod` |
| Retry (spout + producer) | `persistent://ci-preprod/ci-crawl-enricher/ci-crawl-events-retry-preprod` |
| DLQ | `persistent://ci-preprod/ci-crawl-enricher/ci-crawl-events-dlq` |

## Topology overview

1. **Raw spout** + **Retry spout** → `AsyncEnrichmentBolt` (grouped by `KEY`)
2. **AsyncEnrichmentBolt** → **L2** (`EnrichIdsRequest` / `IdRef`); on success → `CentralPublisherBolt`; on failure → `RetryBolt` (`RETRY_STREAM`)
3. **CentralPublisherBolt** → consolidated JSON → **central** topic (via `CentralTopicProducer`)
4. **RetryBolt** → retry topic or `DLQBolt` (`DLQ_STREAM`) when attempts exhausted

## Configuration highlights

- `l2ClientConfig`: `baseUrl`, `enrichPath`, timeouts
- `retryConfig`: `maxAttempts` (default 3 in code if unset)
- `pulsarTopics`: central / retry / DLQ topic names for producers
- Viesti blocks: `pulsarClientConfig`, `rawPulsarSpoutConfig`, `retryPulsarSpoutConfig`, `pulsarDlqConfig`

## Project layout

```text
debian/          confd templates + DEBIAN metadata (fk-3p-storm packaging)
config/          sample CrawlEnrichmentTopology YAML for operators
scripts/         GCP instance templates, make-deb.sh, optional bucket-key JSON
src/main/java/com/flipkart/crawl/ingestion/topology/
  TopologyBooter.java, EnrichmentTopologyBuilder.java, Constants.java
  bolt/          AsyncEnrichmentBolt, CentralPublisherBolt, RetryBolt, DLQBolt
  client/        L2Client, L2HttpClient, *TopicProducer
  config/        EnrichmentTopologyConfig, L2ClientConfig, RetryConfig, ...
  guice/         GuiceEnableHook, TopologyModule
  routing/       PipelineRoutingRegistry, JdbcPipelineRoutingRegistry
  spout/         SpoutFactory, RawEventMapper
  util/          RetryUtil, ConsolidatedPayloadBuilder, RoutingTagsBuilder, ...
  model/         RawEvent, EnrichedEvent, IdRef, EnrichIdsRequest, ...
```

## Status

- **Producers** (`CentralTopicProducer`, `RetryTopicProducer`, `DlqTopicProducer`) may be stubs until wired to a real Pulsar client.
- Align **L2** request/response JSON with your service contract if it differs from the current `EnrichIdsRequest` / `ConsolidatedPayloadBuilder` assumptions.


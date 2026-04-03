# Layer 1 — Low-Level Design (crawl-ingestion-storm)

Apache Storm topology that ingests crawl events from Pulsar, calls the Layer 2 enrichment service (IDs-only HTTP), merges routing tags and L2 data, publishes to a central Pulsar topic, and routes persistent failures through a retry topic to a DLQ.

## Scope

- **In:** raw crawl topic + retry topic (same tuple shape after mapping). Preprod names: tenant `ci-preprod`, namespace `ci-crawl-enricher` — see `enrichment-topology.yaml` (e.g. raw `ci-crawl-events-raw-preprod`, retry `ci-crawl-events-retry-preprod`).
- **Out:** enriched central topic (`ci-enriched-events-preprod` via `CentralTopicProducer`), retry + DLQ topics — same YAML.
- **Guice:** `GuiceEnableHook` installs `TopologyModule` (config, `L2Client` → `L2HttpClient`, `PipelineRoutingRegistry` → `JdbcPipelineRoutingRegistry`, producer singletons).
- **Metrics:** Bolts register `TopologyMetrics` counters (`MetricsNames`: enrich, validation, central/retry/DLQ publish, DLQ emit).
- **Validation:** `AsyncEnrichmentBolt` runs `ValidationUtil` on inner crawl JSON before L2; failures go to `RETRY_STREAM` like L2 errors.

## Topology graph

| From | To | Grouping / stream |
|------|----|-------------------|
| Raw spout | `AsyncEnrichmentBolt` | `fieldsGrouping(KEY)` |
| Retry spout | `AsyncEnrichmentBolt` | `fieldsGrouping(KEY)` |
| `AsyncEnrichmentBolt` | `CentralPublisherBolt` | `fieldsGrouping(KEY)` — default stream |
| `AsyncEnrichmentBolt` | `RetryBolt` | `fieldsGrouping(KEY)` — `RETRY_STREAM` |
| `RetryBolt` | `DLQBolt` | `fieldsGrouping(KEY)` — `DLQ_STREAM` |

Built in `EnrichmentTopologyBuilder`.

## Components

### Spouts (`SpoutFactory` + `RawEventMapper`)

- **Raw:** consumer on configured raw crawl topic.
- **Retry:** consumer on configured retry topic.
- **Mapper:** emits `(KEY, RAW_EVENT)`; `KEY` from JSON `product_id`, else Pulsar key, else message id.

### `AsyncEnrichmentBolt`

- Extends **`BaseWindowedBolt`** (tumbling window: `l2ClientConfig.microBatchWindowCount` by default, or set count to `0` and use `microBatchWindowSeconds`).
- Input: `KEY`, `RAW_EVENT` (plain crawl JSON or retry **envelope** JSON).
- Unwraps inner crawl JSON via `RetryUtil.extractRawEventJson` for L2.
- Batches valid tuples per window into **`L2Client.enrichBatch`** (sub-batches capped by `maxBatchSize`); **`L2HttpClient`** sends **IDs only** (`EnrichIdsRequest` / `IdRef`).
- **Success:** one emit per input tuple: `(KEY, inner RAW_EVENT, ENRICHED_EVENT)` with `ENRICHED_EVENT` sliced from the batch response (`enriched_events[i]` order must match the request).
- **Failure:** emits `RETRY_STREAM` `(KEY, RAW_EVENT)` preserving the full tuple payload for attempt tracking.

### `CentralPublisherBolt`

- Input: `KEY`, inner crawl JSON (`RAW_EVENT`), L2 response (`ENRICHED_EVENT`).
- Builds consolidated JSON via `ConsolidatedPayloadBuilder.build` (raw + routing tags from crawl + optional **`pipelines`** from `PipelineRoutingRegistry` + L2 `match_status` / `fk_data` / optional L2 `routing_tags`).
- Publishes with `CentralTopicProducer.publish(config, key, consolidatedJson)`.

### `RetryBolt`

- Input: `RETRY_STREAM` — `(KEY, RAW_EVENT)`.
- Computes `nextAttempt = currentAttempt + 1` using `RetryUtil` (envelope-aware).
- If `nextAttempt >= maxAttempts` (default 3): emits `DLQ_STREAM` `(KEY, RAW_EVENT, attempt)`.
- Else: `RetryUtil.wrapWithAttempt(innerRaw, nextAttempt)` and `RetryTopicProducer.publish`.

### `DLQBolt`

- Input: `DLQ_STREAM` — `(KEY, RAW_EVENT, attempt)`.
- `DlqTopicProducer.publish`.

## Tuple & stream constants

See `Constants`: `KEY`, `RAW_EVENT`, `ENRICHED_EVENT`, `RETRY_STREAM`, `DLQ_STREAM`.

## Configuration (`EnrichmentTopologyConfig`)

- Viesti/Pulsar: `pulsarClientConfig`, `rawPulsarSpoutConfig`, `retryPulsarSpoutConfig`, `pulsarDlqConfig`.
- Topics for producers: `pulsarTopics` (`PulsarConfig`: central, retry, DLQ).
- `l2ClientConfig` (including micro-batch window + `maxBatchSize`), optional `pipelineRouting` (MySQL classifier), `retryConfig`, bolt `BaseStormUnitConfig` entries (name + parallelism).
- YAML: `src/main/resources/enrichment-topology.yaml` (fill viesti blocks per environment).

## Out of scope / stubs

- **Pulsar producers** may log only until real client + publish is wired.
- **gRPC** to L2 is not implemented; HTTP via `L2HttpClient` only.

## Package layout (main)

`com.flipkart.crawl.ingestion.topology` — booter, builder, constants, `bolt`, `client`, `config`, `guice`, `metrics`, `model`, `routing`, `spout`, `util`.

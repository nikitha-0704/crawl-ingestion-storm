# Layer 1 — Sequence flows

## Happy path (raw spout → central topic)

```mermaid
sequenceDiagram
    participant P as Pulsar raw topic
    participant S as Raw spout + RawEventMapper
    participant E as AsyncEnrichmentBolt
    participant L2 as Layer 2 HTTP
    participant C as CentralPublisherBolt
    participant Out as Pulsar central topic

    P->>S: message (crawl JSON)
    S->>E: tuple (KEY, RAW_EVENT)
    E->>E: extractRawEventJson (inner crawl)
    E->>L2: POST IDs-only body (EnrichIdsRequest)
    L2-->>E: enriched JSON
    E->>C: tuple (KEY, RAW_EVENT inner, ENRICHED_EVENT)
    C->>C: ConsolidatedPayloadBuilder (routing tags + merge)
    C->>Out: publish consolidated JSON
```

## L2 failure → retry topic

```mermaid
sequenceDiagram
    participant E as AsyncEnrichmentBolt
    participant R as RetryBolt
    participant RT as Retry topic producer
    participant PT as Pulsar retry topic

    E->>E: L2 throws
    E->>R: RETRY_STREAM (KEY, RAW_EVENT)
    R->>R: nextAttempt = current + 1
    alt nextAttempt < maxAttempts
        R->>RT: publish envelope JSON
        RT->>PT: message
    else nextAttempt >= maxAttempts
        R->>R: emit DLQ_STREAM
    end
```

## Retry spout re-enters enrich

```mermaid
sequenceDiagram
    participant PT as Pulsar retry topic
    participant RS as Retry spout + RawEventMapper
    participant E as AsyncEnrichmentBolt
    participant L2 as Layer 2 HTTP

    PT->>RS: envelope message
    RS->>E: (KEY, RAW_EVENT = envelope string)
    E->>E: extractRawEventJson → inner crawl for L2
    E->>L2: POST IDs-only
    L2-->>E: response or error
```

## DLQ path

```mermaid
sequenceDiagram
    participant R as RetryBolt
    participant D as DLQBolt
    participant DP as DLQ producer
    participant P as Pulsar DLQ topic

    R->>D: DLQ_STREAM (KEY, RAW_EVENT, attempt)
    D->>DP: publish
    DP->>P: message
```

## Attempt counting (conceptual)

1. First L2 failure: tuple is **plain** crawl JSON → `currentAttempt = 0` → `nextAttempt = 1` → publish **envelope** with `attempt: 1` to retry topic.
2. Retry consumer delivers envelope → enrich unwraps inner JSON for L2.
3. On repeated L2 failure, `nextAttempt` increases until it reaches `maxAttempts` → **DLQ** (no further retry publish).

See `RetryUtil` and `RetryBolt` for the exact rules.

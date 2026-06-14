# DriftGuard Demo

DriftGuard Demo is a standalone Spring Boot application that consumes the DriftGuard library in two production-like integration styles:

- `Checkout Service`: a direct embedded integration where business operations publish `MetricPoint` values to DriftGuard in-process.
- `Kafka Service`: a stream-processing integration where service producers publish `MetricPoint` messages to Kafka and a Kafka Streams topology emits `DriftEvent` alerts.

## Prerequisite

Install the local DriftGuard library artifacts first:

```bash
cd ../DriftGuard
./mvnw install
```

## Run Locally

```bash
cd ../DriftGuardDemo
./mvnw spring-boot:run
```

The UI is available at `http://localhost:8080`.

`Checkout Service` works with only the Spring Boot app. It publishes observations through DriftGuard's `MetricPointPublisher` port and receives alerts through the starter-managed alert sinks. The starter provides SLF4J logging by default; the demo adds a repository sink so alerts are visible in the UI.

The demo also enables DriftGuard's built-in webhook sink. It posts alert JSON to `/internal/alerts/driftguard`, a local stand-in for a production incident router or chat-bot endpoint. In a real service the same `driftguard.alerts.webhook.*` settings would point to an external URL; custom delivery is done by adding another `DriftAlertSink` bean.

Synthetic benchmark screens use `driftguard-testkit`: scenarios generate reproducible metric streams, benchmark runners calculate precision/recall and first-detection delay, and reports can be rendered to Markdown for review notes.

`Research Lab` extends this into a reproducible paired experiment. It compares three fixed detector profiles with an adaptive strategy that selects a profile from baseline variability and lag-one autocorrelation. The experiment varies scenario type, noise, effect size and random seed, then reports F1, precision, recall, false positives per 1,000 observations, detection delay and 95% confidence intervals.

The research methodology, hypotheses and validity limitations are documented in [RESEARCH.md](RESEARCH.md).

`Kafka Service` needs a reachable Kafka broker; use the full Docker stack below for the complete two-scenario demo.

The checkout screen is organized like a small operations console: summary cards, manual operations, runtime pipeline, metrics grouped by signal type, operation-level aggregates, recent operation log and emitted alerts.

Synthetic and overview lab screens are still available for algorithm demonstrations, but they are hidden by default. Enable them with `?lab=1` or by setting `localStorage.driftguard.showLab = "true"` in the browser.

## Full Stack

```bash
docker compose up --build
```

The Docker build uses the sibling `../DriftGuard` checkout as a BuildKit context,
installs the library inside the Maven build stage and then packages this demo app.

Services:

- Demo UI: `http://localhost:8080`
- Kafka: `localhost:9092`
- Kafka UI: `http://localhost:8090`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin` / `admin`)

## Frontend Development

```bash
cd src/main/frontend
npm ci
npm run dev
```

Vite proxies `/api`, `/actuator`, `/v3` and Swagger requests to `localhost:8080`.

## Research API

- `POST /api/research` starts an asynchronous experiment.
- `GET /api/research` returns progress and the latest completed report.
- `POST /api/research/cancel` requests cancellation.
- `GET /api/research/export.csv` exports aggregate results.
- `GET /api/research/export.md` exports a report suitable for review notes.

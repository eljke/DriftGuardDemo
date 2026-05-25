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

`Checkout Service` works with only the Spring Boot app. `Kafka Service` needs a reachable Kafka broker; use the full Docker stack below for the complete two-scenario demo.

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

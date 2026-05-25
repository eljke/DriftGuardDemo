# DriftGuard Demo

DriftGuard Demo is a standalone Spring Boot checkout service that consumes the DriftGuard library. The default UI runs real demo operations, publishes operational metrics, lets DriftGuard detect drift and stores recent alert events.

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

Synthetic and Kafka lab screens are still available for algorithm demonstrations, but they are hidden by default. Enable them with `?lab=1` or by setting `localStorage.driftguard.showLab = "true"` in the browser.

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

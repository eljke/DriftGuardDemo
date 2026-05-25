# DriftGuard Demo

DriftGuard Demo is a standalone Spring Boot observability demo that consumes the DriftGuard library. It simulates service metrics, runs drift detection, stores recent drift events and exposes a React UI plus REST/OpenAPI endpoints.

Before building this project from a local checkout, install the DriftGuard library artifacts:

```bash
cd ../DriftGuard
./mvnw install
```

Then run the demo:

```bash
cd ../DriftGuardDemo
./mvnw spring-boot:run
```

The UI is available at `http://localhost:8080`.

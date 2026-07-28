# inventory-reservation

Spring Boot service that temporarily reserves product inventory during checkout to prevent overselling.

| | |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.6 |
| **Build** | Gradle (wrapper) |
| **Storage (MVP)** | In-memory (`ConcurrentHashMap` + per-product locks) |
| **Module** | Single module |

## Overview

Customers can reserve one or more units of a product, then confirm (purchase) or cancel. Expired holds are released by a scheduled sweep. Controllers call a service that depends on repository interfaces so storage can later move to Redis/DB without changing API contracts.

**Story:** [SCRUM-5](https://pkvanga1508.atlassian.net/browse/SCRUM-5)

## Architecture Decision Records (ADRs)

| ADR | Title | Link |
|-----|--------|------|
| ADR-001 | In-memory per-product locking with repository boundary | [Confluence](https://pkvanga1508.atlassian.net/wiki/spaces/SD/pages/622593) |

## Prerequisites

- JDK 21
- Network access for first Gradle dependency download (Maven Central)

## Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `reservation.ttl-seconds` | Reservation lifetime in seconds | `600` |
| `reservation.expiry-sweep-ms` | How often expired reservations are released | `5000` |
| `server.port` | HTTP port (Spring default) | `8080` |

Set via `src/main/resources/application.properties` or override with env-style Spring props (e.g. `RESERVATION_TTL_SECONDS`).

## How to Run

### Terminal

```bash
cd inventory-reservation
./gradlew bootRun
```

### IDE

1. Open `inventory-reservation` as a Gradle project (IntelliJ / VS Code + Java extension).
2. Use JDK 21 toolchain.
3. Run `com.praveen.www.Application`.

### Docker

Not packaged yet (no `Dockerfile` in this repo). Use Terminal/`bootRun` for local runs.

## Quick Start Example

On startup the app seeds:

- `SKU-100` — Wireless Headphones — 50 units  
- `SKU-200` — USB-C Cable — 100 units  

```bash
# List stock
curl -s http://localhost:8080/api/v1/products | jq

# Reserve 2 units
curl -s -X POST http://localhost:8080/api/v1/reservations \
  -H 'Content-Type: application/json' \
  -d '{"productId":"SKU-100","quantity":2}' | jq

# Confirm (replace RESERVATION_ID)
curl -s -X POST http://localhost:8080/api/v1/reservations/RESERVATION_ID/confirm | jq

# Or cancel
curl -s -X POST http://localhost:8080/api/v1/reservations/RESERVATION_ID/cancel | jq
```

## API Endpoints (summary)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/products` | List product stock |
| GET | `/api/v1/products/{productId}` | Get one product |
| POST | `/api/v1/products` | Seed / upsert product stock |
| POST | `/api/v1/reservations` | Create reservation |
| POST | `/api/v1/reservations/{id}/confirm` | Confirm (consume stock) |
| POST | `/api/v1/reservations/{id}/cancel` | Cancel (release stock) |

Full request/response and error codes: [API One Pager](Documentations/API_ONE_PAGER.md)

## Project Structure

```
inventory-reservation/
├── build.gradle
├── settings.gradle
├── src/main/java/com/praveen/www/
│   ├── Application.java
│   ├── domain/          # Product, Reservation, status
│   ├── dto/             # Request/response DTOs
│   ├── exception/       # Domain errors + GlobalExceptionHandler
│   ├── repository/      # Interfaces + in-memory impls
│   ├── service/         # ProductService, ReservationService
│   ├── config/          # TTL properties, Clock, scheduling
│   └── web/             # REST controllers
├── src/main/resources/application.properties
├── src/test/java/       # Unit + MockMvc integration tests
├── Documentations/
│   └── API_ONE_PAGER.md
└── README.md
```

## Development

```bash
./gradlew build          # compile + all tests
./gradlew test           # tests only
./gradlew bootRun        # run app
```

## Troubleshooting

### Port 8080 already in use
Stop the other process or run with `--server.port=8081`.

### Inventory “lost” after restart
Expected for MVP — state is in-memory only. See [ADR-001](https://pkvanga1508.atlassian.net/wiki/spaces/SD/pages/622593).

### Reservation expired / 409 INVALID_RESERVATION_STATE
Default TTL is 600 seconds. Confirm or cancel before expiry, or raise `reservation.ttl-seconds` for local demos.

## Documentation

- [API One Pager](Documentations/API_ONE_PAGER.md)
- [ADR-001 (Confluence)](https://pkvanga1508.atlassian.net/wiki/spaces/SD/pages/622593)
- [SCRUM-5](https://pkvanga1508.atlassian.net/browse/SCRUM-5)

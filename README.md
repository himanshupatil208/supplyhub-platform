
# SupplyHub — B2B Supply Chain & Order Management Platform

A portfolio-sized full-stack application built around independently deployable Spring Boot microservices.

## Architecture

- **API Gateway** (`8080`) — single entry point; routes `/api/catalog/**`, `/api/inventory/**`, `/api/customers/**`, `/api/orders/**`, and `/api/notifications/**`.
- **Catalog Service** (`8081`) — product catalog management.
- **Inventory Service** (`8082`) — stock levels, stock adjustments, and reservations.
- **Customer Service** (`8083`) — distributor customer records.
- **Order Service** (`8084`) — order creation, line items, totals, status transitions, and inventory reservation through a service-to-service REST call.
- **Notification Service** (`8085`) — records operational events and notifications.
- **Frontend** (`5173`) — React + TypeScript operations dashboard.
- **PostgreSQL** — one local server with a separate schema per service.

Each backend service is a separate Spring Boot application and can be built and deployed independently. The services use their own schema and own their data. The order service stores order snapshots and calls the inventory service when an order is placed.

## Run locally

Requirements: Docker Desktop (or Docker Engine + Compose), Java 21 and Maven if running services outside Docker, Node.js 20+ if running the frontend outside Docker.

```bash
docker compose up --build
```

Open:
- Frontend: http://localhost:5173
- API Gateway: http://localhost:8080
- Health: http://localhost:8080/actuator/health

The first startup may take a minute while Maven dependencies are downloaded and the database initializes.

## Run without Docker

1. Start PostgreSQL and create database `supplyhub`.
2. Create schemas: `catalog`, `inventory`, `customers`, `orders`, `notifications`.
3. In each service directory run `mvn spring-boot:run` with the corresponding `SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`.
4. Start the gateway with `mvn spring-boot:run`.
5. In `frontend`, run `npm install && npm run dev`.

The default local datasource URL is `jdbc:postgresql://localhost:5432/supplyhub?currentSchema=<schema>`, username `supplyhub`, password `supplyhub`.

## Useful API examples

```bash
# Create a customer
curl -X POST http://localhost:8080/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"North Star Market","email":"orders@northstar.example","phone":"716-555-0100","company":"North Star Foods"}'

# Create a product
curl -X POST http://localhost:8080/api/catalog/products \
  -H 'Content-Type: application/json' \
  -d '{"sku":"TOM-001","name":"Roma Tomatoes","description":"Fresh Roma tomatoes, 25 lb case","category":"Produce","unitPrice":24.50,"active":true}'

# Add inventory
curl -X POST http://localhost:8080/api/inventory/stock \
  -H 'Content-Type: application/json' \
  -d '{"sku":"TOM-001","productName":"Roma Tomatoes","availableQuantity":100,"reorderLevel":20,"warehouse":"Buffalo"}'

# Place an order (inventory is reserved by the Order Service)
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":1,"customerName":"North Star Market","items":[{"sku":"TOM-001","productName":"Roma Tomatoes","quantity":2,"unitPrice":24.50}]}'
```

## Design notes

- DTO-based REST boundaries; persistence entities are not exposed by the API.
- Bean Validation on incoming requests.
- Transactional writes inside each service.
- Service-to-service communication uses HTTP via `RestClient`.
- Inventory reservation is validated before an order is persisted. For a production system, use an outbox + message broker and a saga/compensation flow for distributed transactions.
- The payment integration is intentionally simulated; no real payment credentials or payment processing are included.
- CORS is enabled for the local Vite development server.
- H2 is not used: PostgreSQL is the source of truth.

## Next production-hardening steps

Add OAuth2/OIDC authentication, role-based authorization, Kafka/RabbitMQ events, an outbox pattern, idempotency keys, distributed tracing (OpenTelemetry), centralized secrets, database migrations (Flyway), contract tests, and CI/CD.

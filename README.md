# Yard Management System (YMS) — Backend

This is the backend service for the Yard Management System (YMS). It provides the core business logic, database migrations, real-time messaging, scheduled tasks, and RESTful APIs for the YMS platform.

---

## 🛠 Technology Stack

- **Language & Framework**: Java 25 (LTS), Spring Boot `3.5.x`
- **Security & Auth**: Spring Security, JWT, BCrypt, RBAC
- **Persistence & Migration**: Spring Data JPA, Hibernate, Flyway
- **Business Rules Engine**: Drools (daily interest calculation)
- **Task Scheduling**: Quartz Scheduler (daily report generation & email distribution)
- **Primary Database**: PostgreSQL 16 (with optional TimescaleDB & PostGIS extensions)
- **Caching & Locks**: Redis 7
- **Messaging & Event Streaming**: Apache Kafka (KRaft), EMQX MQTT Broker (STOMP `/ws` bridge)
- **Object Storage**: MinIO / S3-compatible local storage
- **API Documentation**: OpenAPI 3 / Swagger UI (`springdoc-openapi`)

---

## 📁 Package Structure (Package-by-Domain)

The backend follows a domain-driven, package-by-domain architecture. This keeps domain entities, controllers, services, and repositories localized, simplifying future microservice extraction (M1–M18).

```text
backend/yms-service/src/main/java/com/yms/yms/
├── YmsApplication.java
├── domain/
│   ├── user/              # User management & authentication context
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   └── entity/
│   ├── partner/           # Partner A, B, C management
│   ├── contract/          # Partner contracts & auto-expiry jobs
│   ├── yard/              # Yard layout, capacity, check-in/out, logs
│   ├── tracking/          # GPS tracking, geofence, route logs
│   ├── goods/             # Item QR generation, reconciliation & discrepancies
│   ├── confirmation/      # Driver arrival/departure SLAs & signature uploads
│   ├── finance/           # Debts, partial payments, interest ledger & invoices
│   └── report/            # Dashboard stats, Excel/PDF exports & Quartz schedules
└── infrastructure/
    ├── config/            # Spring beans, OpenAPI, CORS, and data seeds
    ├── security/          # JWT filters, Spring Security, UserDetails adapter
    ├── audit/             # Immutable audit table triggers & @Audit aspect
    └── storage/           # MinIO / Object Storage integration
```

---

## 🚀 Getting Started

### 1. Prerequisites

Ensure local infrastructure services are running via Docker Compose (from the project root):

```bash
docker compose -f infra/docker-compose.yml up -d
```

### 2. Default Configuration & Credentials

| Property | Default Development Value |
|---|---|
| **Database** | `jdbc:postgresql://localhost:5432/yms` |
| **DB Username / Password** | `yms` / `yms` |
| **Redis** | `localhost:6379` |
| **Kafka** | `localhost:9092` |
| **EMQX MQTT** | `localhost:1883` |
| **MinIO** | `localhost:9000` (Console: `localhost:9001`) |
| **JWT Secret** | `dev-only-change-me-please-use-at-least-32-characters` |

#### Seeded Seed Admin Account
- **Username**: `admin`
- **Password**: `Admin@123`
- **Role**: `ADMIN`

> **Note**: Migration `V4__repair_admin_seed_password.sql` automatically repairs untouched bootstrap hashes. Always change the default admin password and JWT secret before deploying to staging/production.

---

## ⚙️ Environment Variables & Feature Flags

| Variable | Description | Default |
|---|---|---|
| `QR_HMAC_SECRET` | HMAC key for signing QR tokens (min 32 bytes) | Uses JWT secret if unset |
| `POSTGIS_ENABLED` | Enable PostGIS spatial functions & geography columns | `false` |
| `TRACKING_ENABLED` | Enable MQTT/Kafka GPS listeners | `true` |
| `MINIO_ENABLED` | Enable MinIO storage for signatures and files | `false` |
| `MINIO_ENDPOINT` | MinIO server URL | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO access key | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO secret key | `minioadmin` |
| `MINIO_BUCKET` | MinIO bucket name | `yms-storage` |
| `E_INVOICE_ENABLED` | Enable submission to external e-invoice provider | `false` |
| `INVOICE_STORAGE_PATH` | Storage directory path for generated invoice PDFs | `./storage/invoices` |

---

## 🔨 Build & Run Commands

Execute commands from the `/backend` directory:

### Compile & Package
```bash
mvn clean package
```

### Run Service
```bash
mvn -pl yms-service spring-boot:run
```

### Run Unit & Core Integration Tests
```bash
mvn -pl yms-service -am test
```

### Run PostgreSQL Docker Integration Tests
The backend includes PostgreSQL integration tests using Testcontainers. These start an isolated PostgreSQL 16 container, execute all Flyway scripts (`V1` to `V6`), and verify database operations.

```bash
mvn -pl yms-service -Dtest=PostgresDatabaseIntegrationTest test
```
*(Requires Docker daemon to be running. Automatically skipped if Docker is unavailable).*

---

## 📖 Swagger UI & API Documentation

Interactive API testing and documentation are available once the backend service is running:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Health Check**: `GET http://localhost:8080/actuator/health`

### Testing Protected Endpoints in Swagger
1. Call `POST /api/auth/login` with the default admin credentials.
2. Copy the `accessToken` string from the JSON response.
3. Click the **Authorize** button at the top right of Swagger UI.
4. Paste the token into the value field and click **Authorize**.

---

## 🌐 API Route Reference

### Authentication & Base Operations

| Method | Route | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/auth/login` | Authenticate user & receive JWT access token | Public |
| `GET` | `/api/admin/ping` | Verify admin authentication status | `ADMIN` |

---

### Sprint 2 — Yard Operations (AIT-401–808)

Flyway migration: `V3__yard_operations.sql`

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/api/vehicles/pre-notifications` | Register vehicle pre-notification & advance services |
| `GET` | `/api/vehicles/pre-notifications/waiting` | List vehicles scheduled for arrival |
| `POST` | `/api/vehicles/pre-notifications/{id}/qr` | Generate signed QR token & QR image |
| `POST` | `/api/yard-logs/scan` | Validate QR scan and fetch vehicle details |
| `POST` | `/api/yard-logs/check-in` | Confirm entry and assign yard space |
| `POST` | `/api/yard-logs/check-out` | Record vehicle departure and calculate total duration |
| `POST` / `GET` | `/api/service-orders` | Assign service orders or query active service status |
| `POST` | `/api/service-orders/{id}/start` | Mark service start time |
| `POST` | `/api/service-orders/{id}/end` | Mark service completion & calculate hourly charges |
| `GET` / `POST` | `/api/yards` | List all yards / create new yard |
| `PUT` / `DELETE`| `/api/yards/{id}` | Update yard details / deactivate yard |
| `GET` | `/api/yards/inventory` | View real-time capacity and current yard occupancy |
| `GET` | `/api/yards/alerts` | Get active alerts for yards exceeding 90% capacity |

---

### Sprint 3 — Tracking, Goods QR & Driver Confirmations (AIT-1101–1306)

Flyway migration: `V5__tracking_goods_confirmations.sql`

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/api/tracking/gps` | Ingest raw GPS telemetry point |
| `GET` | `/api/tracking/trips/{tripId}/history` | Fetch historical trip coordinates (up to 90 days) |
| `GET` | `/api/tracking/trips/{tripId}/latest` | Fetch most recent GPS location for a trip |
| `GET` / `POST` | `/api/tracking/trips/{tripId}/geofences` | View or create circular geofence parameters |
| `GET` | `/api/tracking/alerts` | List route deviation and geofence breach alerts |
| `POST` | `/api/goods/batch` | Register trip goods with unique HMAC-signed QR values |
| `GET` | `/api/goods?tripId={tripId}` | Retrieve itemized goods registered to a specific trip |
| `GET` | `/api/goods/{goodsId}/qr` | Download QR image for a specific goods item |
| `POST` | `/api/goods/{goodsId}/reconcile` | Compare scanned QR item quantities against manifest |
| `GET` | `/api/goods/discrepancies` | Get trip discrepancy report for mismatched quantities |
| `POST` | `/api/confirmations/trips/{tripId}` | Request driver departure/arrival with a 30-min SLA |
| `GET` | `/api/confirmations?tripId={tripId}` | Fetch confirmation status for a trip |
| `POST` | `/api/confirmations/{id}/confirm` | Confirm driver arrival/departure event |
| `POST` | `/api/confirmations/{id}/signature` | Upload PNG/JPEG driver signature (max 5 MB) |
| `GET` | `/api/confirmations/alerts` | Get alerts for overdue/unconfirmed SLA requests |

---

### Sprint 4 — Finance, Invoices & Reports (AIT-1401–1608)

Flyway migration: `V6__finance_invoicing_reports.sql`

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/finance/debts?partnerId={id}` | Query partner outstanding balances & interest ledger |
| `PUT` | `/api/finance/interest-config/{partnerId}` | Set partner daily interest rate & grace periods |
| `POST` | `/api/finance/invoices` | Issue immutable service invoice for completed orders |
| `GET` | `/api/finance/invoices` | Query issued invoices list |
| `GET` | `/api/finance/invoices/{id}/pdf` | Generate and download invoice PDF file |
| `POST` | `/api/finance/invoices/{id}/payments` | Record payment transfer & optional proof (max 10 MB) |
| `POST` | `/api/finance/invoices/{id}/e-invoice` | Submit invoice to external e-invoice provider |
| `GET` | `/api/finance/reconcile` | Compare billing totals between Partner A and Partner C |
| `GET` | `/api/reports/monthly` | Aggregate monthly revenue, yard capacity & debts |
| `GET` | `/api/reports/quarterly` | Aggregate quarterly financial & operational statistics |
| `GET` | `/api/reports/dashboard` | Fetch current operational KPI snapshot |
| `GET` | `/api/reports/export.xlsx` | Export filtered invoice report to Excel (max 100k rows)|
| `GET` | `/api/reports/export.pdf` | Export filtered invoice report to PDF |
| `GET` / `POST` | `/api/reports/schedules` | View or configure Quartz daily automated report emails |
| `DELETE` | `/api/reports/schedules/{id}` | Delete scheduled report email entry |

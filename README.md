# crm-small

CRM for IT services — LTV and RFM analytics.

## Prerequisites

- Java 21
- Maven 3.x
- PostgreSQL 14+

## Database Setup

Create the database and user:

```sql
CREATE DATABASE crm_small;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE crm_small TO postgres;
```

The default connection expects PostgreSQL running on `localhost:5432` with:

| Property | Value     |
|----------|-----------|
| Host     | localhost |
| Port     | 5433      |
| Database | crm_small |
| Username | postgres  |
| Password | postgres  |

To use different credentials, edit `src/main/resources/application.properties`.

Schema migrations are applied automatically on startup via Flyway.

## Running the Application

```bash
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw package -DskipTests
java -jar target/crm-small-0.0.1-SNAPSHOT.jar
```

The API starts on `http://localhost:8080`.

## Running Tests

```bash
./mvnw test
```

Tests use Testcontainers — Docker must be running.

## UI

Static HTML pages are located in the `ui/` directory:

- `dashboard.html` — main dashboard
- `companies.html` — company list
- `company-detail.html` — company detail view
- `analytics-rfm.html` — RFM analytics

Open these files directly in a browser while the backend is running.

## Analytics

LTV and RFM scores are recalculated nightly at 02:00 AM (configurable via `crm.analytics.cron` in `application.properties`).

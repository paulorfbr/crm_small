# Research: crm-small Phase 1 — Architecture Decisions

**Feature**: crm-small Phase 1 Foundation
**Date**: 2026-04-10

## Decision Log

### D-001: Layered Package Architecture

**Decision**: `api` → `service` → `domain` + `repository` (classic layered
architecture within a single Maven module).

**Rationale**: The application is a single-tenant, small-portfolio CRM with
no inter-service communication. A monolithic layered structure avoids
unnecessary complexity while remaining easy to navigate. Domain objects are
plain JPA entities; no separate DTO layer at the domain level (DTOs live as
inner `record` types on each controller to keep co-location).

**Alternatives considered**:
- Hexagonal / ports-and-adapters: rejected — over-engineering for current
  scope; adds indirection without measurable benefit at this scale.
- Per-aggregate sub-modules: rejected — Maven multi-module adds build
  complexity with no isolation benefit for a single-tenant app.

---

### D-002: PostgreSQL NTILE(5) for RFM Scoring

**Decision**: RFM scores are computed in a single native SQL query using
PostgreSQL `NTILE(5)` window functions. Scores are portfolio-relative
(percentile-based), not fixed thresholds.

**Rationale**: NTILE ensures scores remain meaningful regardless of portfolio
size or distribution. A fixed threshold (e.g., "monetary > $10k = score 5")
becomes stale as the portfolio grows. Native SQL is mandatory because JPA/JPQL
cannot express window functions.

**Alternatives considered**:
- Fixed thresholds: rejected — becomes miscalibrated as portfolio composition
  changes; requires manual tuning.
- Application-side scoring (sort + bucket in Java): rejected — moves large
  result sets to the JVM; PostgreSQL handles it more efficiently in-place.

---

### D-003: Analytics Pipeline Execution Order

**Decision**: RFM → LTV → BCG, always in this order. LTV reads the RFM
segment to determine churn risk discount; BCG reads current-year revenue
from paid events (same source as RFM monetary). The scheduler runs nightly
at 02:00 AM; a manual trigger is available at `POST /api/analytics/recalculate`.

**Rationale**: LTV churn risk discount depends directly on the RFM segment
computed in the same run. Running LTV before RFM would use stale segments.
BCG is independent of RFM/LTV but placed last for consistency.

**Alternatives considered**:
- Parallel execution: rejected — LTV has a hard dependency on RFM output;
  parallelism would require a barrier after RFM anyway, adding complexity.

---

### D-004: Testcontainers for Integration Tests

**Decision**: All integration tests spin up a real PostgreSQL container via
Testcontainers. No H2 in-memory database, no Mockito mocking of the
`EntityManager` or repositories.

**Rationale**: The analytics service uses native SQL with window functions
(`NTILE`, `EXTRACT`, `::SMALLINT` casts) that are PostgreSQL-specific and
not supported by H2. Mocking the database layer would give false confidence
that queries work. Testcontainers provides an identical PostgreSQL environment
at the cost of slightly longer test startup (mitigated by container reuse).

**Alternatives considered**:
- H2 with PostgreSQL compatibility mode: rejected — does not support
  `NTILE`, custom casts, or several PostgreSQL-specific SQL constructs.
- Mocking `EntityManager`: rejected — explicitly prohibited by the constitution
  (Principle III).

---

### D-005: `ApiResponse<T>` Envelope

**Decision**: All REST responses are wrapped in a generic `ApiResponse<T>`
record containing `success`, `data`, and `error` fields.

**Rationale**: A uniform envelope lets the static HTML UI and any future
integration consumer handle errors and data with a single parsing pattern,
without endpoint-specific error handling.

**Alternatives considered**:
- Spring's `ResponseEntity<T>`: rejected — leaks HTTP mechanics into the
  service layer and makes error/success distinction inconsistent across
  endpoints.
- RFC 9457 Problem Details: deferred — appropriate for Phase 4 when the API
  is exposed to external consumers.

---

### D-006: Flyway for Schema Migrations

**Decision**: All DDL managed through Flyway versioned migrations
(`V1__...sql` through `V6__...sql`). `spring.jpa.hibernate.ddl-auto=validate`
ensures the schema matches JPA entities at startup but never modifies it.

**Rationale**: Automatic DDL generation (`create-drop`, `update`) is
unpredictable in production and cannot be reviewed or rolled back. Flyway
provides an auditable, ordered history of all schema changes.

**Alternatives considered**:
- Liquibase: equivalent capability; Flyway chosen for simpler YAML/SQL-only
  workflow and smaller footprint.
- `ddl-auto=update`: rejected — silently diverges from reviewed migrations;
  constitution prohibits manual schema changes outside Flyway.

---

### D-007: BCG Portfolio-Median Thresholds

**Decision**: BCG quadrant boundaries are computed as the median of
`revenue_share_pct` and `revenue_growth_rate_pct` across all companies in
the current run, not fixed percentages.

**Rationale**: A fixed 50% revenue share threshold makes no sense for a
portfolio where the top client holds 80% of revenue. Median-based thresholds
adapt to portfolio composition and keep quadrant distribution balanced.

**Alternatives considered**:
- Fixed 50% share / 10% growth thresholds: rejected — arbitrary for IT
  services portfolios where revenue distribution is highly concentrated.

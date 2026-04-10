<!--
SYNC IMPACT REPORT
==================
Version change: [template] → 1.0.0
New document — initial ratification from template.

Modified principles: N/A (first fill)
Added sections:
  - Core Principles (5 principles defined)
  - Technical Standards
  - Development Workflow
  - Governance

Templates requiring updates:
  - .specify/templates/plan-template.md ✅ aligned (Constitution Check section present)
  - .specify/templates/spec-template.md ✅ aligned (no constitutional conflicts)
  - .specify/templates/tasks-template.md ✅ aligned (test-first and phase structure match)
  - .specify/templates/agent-file-template.md ✅ no conflicts

Deferred TODOs: none
-->

# crm-small Constitution

## Core Principles

### I. Domain-Driven Aggregates

The core domain is expressed through five first-class aggregates: **Company**,
**Contact**, **Contract**, **Invoice**, and **Interaction**. Every feature MUST
map cleanly onto one or more of these aggregates. Introducing new top-level
entities requires explicit justification and constitution amendment.

- Companies are the root aggregate; all other entities cascade from them.
- Status transitions (PROSPECT → ACTIVE → AT_RISK → CHURNED) MUST be driven
  by domain events (billing payment, RFM segment change), not by manual UI
  updates alone.
- UUIDs MUST be used as primary keys on all tables (`gen_random_uuid()`).

**Rationale**: A stable aggregate model prevents schema sprawl and keeps the
analytics pipeline anchored to a predictable data shape.

### II. Analytics Pipeline Integrity

The analytics pipeline MUST execute in fixed order: **RFM → LTV → BCG**. Each
stage MUST complete before the next begins. Scores are portfolio-relative
(NTILE/median-based), not fixed thresholds — any hard-coded numeric cutoffs
violate this principle.

- The nightly scheduler runs at 02:00 AM (configurable via
  `crm.analytics.cron`).
- A `POST /api/analytics/recalculate` endpoint MUST exist for manual
  triggering of the full pipeline.
- Intermediate results (RFM scores) MUST be persisted before LTV reads them.
- BCG thresholds MUST be recalculated from portfolio medians on every run.

**Rationale**: Out-of-order or partial runs produce inconsistent LTV and BCG
data, misleading account managers acting on stale segment classifications.

### III. Test-First with Real Infrastructure (NON-NEGOTIABLE)

All tests MUST run against a real PostgreSQL instance via Testcontainers. Mocking
the database layer is prohibited. Tests MUST be written before implementation
(Red → Green → Refactor).

- Unit tests are permitted for pure logic (e.g., scoring formulas, segment
  mapping functions) that carry no I/O.
- Integration tests MUST cover: new aggregate endpoints, analytics pipeline
  execution, and any status-transition logic.
- Docker MUST be available in CI to satisfy Testcontainers requirements.

**Rationale**: The analytics queries use native SQL with window functions
(NTILE). Mock databases cannot validate these queries. A prior incident
pattern — where mock/prod divergence masks broken queries — makes real-DB
testing non-negotiable.

### IV. API Consistency

Every REST response MUST be wrapped in `ApiResponse<T>`. No endpoint may return
a raw entity, collection, or error without this envelope.

- All endpoints follow REST conventions: collections at plural nouns, actions
  as sub-resources (e.g., `/api/invoices/{id}/pay`).
- HTTP status codes MUST accurately reflect the outcome (200/201/204/400/404/
  409/500).
- API format is JSON only; no XML or form-encoded responses.
- Pagination MUST be supported on all list endpoints that may return more than
  50 records.

**Rationale**: A consistent response envelope enables frontend and future
integration consumers to handle errors and data uniformly without endpoint-
specific parsing logic.

### V. Operational Simplicity

Configuration MUST live in `src/main/resources/application.properties`. No
environment-specific config files, no secret management frameworks, and no
feature flags are introduced until the platform reaches Phase 4 (multi-tenant).

- Schema migrations MUST use Flyway and be applied automatically on startup.
- Scheduled jobs MUST log warnings (not throw exceptions) for renewal and
  overdue-invoice alerts — alerting must not abort the analytics pipeline.
- New configuration properties MUST be documented in `README.md` under
  Configuration Reference before merging.

**Rationale**: crm-small targets small PMEs. Operational complexity that
requires DevOps expertise is inappropriate for the current scale and team size.

## Technical Standards

- **Language**: Java 21
- **Framework**: Spring Boot 3.2 with Spring Data JPA + Hibernate
- **Database**: PostgreSQL 14+ (Testcontainers for tests)
- **Migrations**: Flyway — automatic on startup
- **Build**: Maven (`./mvnw`)
- **Analytics queries**: Native SQL with window functions executed server-side
- **UI**: Static HTML pages in `ui/`; no SPA framework until Phase 4

All dependencies MUST be managed via `pom.xml`. Direct JAR additions to the
classpath are prohibited. Dependency upgrades that change a Spring Boot major
version require a constitution amendment.

## Development Workflow

1. **Spec first** — every feature begins with a spec under `specs/`.
2. **Tests before implementation** — per Principle III, tests MUST be written
   and confirmed failing before any production code is added.
3. **Analytics changes require pipeline test** — any change touching RFM, LTV,
   or BCG MUST include an integration test that exercises the full
   recalculation endpoint.
4. **No manual schema changes** — all DDL goes through a Flyway migration file.
5. **Configuration changes** — new `application.properties` keys MUST be
   documented in `README.md` in the same PR.
6. **PR checklist** — reviewer MUST verify: API envelope used, no raw DB mocks
   in tests, Flyway migration present (if schema changed), and analytics
   execution order preserved (if analytics touched).

## Governance

This constitution supersedes all other project conventions. Amendments require:
1. A written rationale documenting the change and its impact.
2. Identification of any principles removed, redefined, or added.
3. A migration plan for existing code that violated the new rule (if any).
4. Version increment per semantic versioning:
   - **MAJOR** — principle removal or backward-incompatible redefinition.
   - **MINOR** — new principle or materially expanded guidance.
   - **PATCH** — clarifications, wording, or non-semantic refinements.

All PRs MUST include a Constitution Check in the plan confirming compliance.
Complexity that violates a principle MUST be explicitly justified in the plan's
Complexity Tracking table. Runtime development guidance lives in the
agent-file generated by `/speckit.plan`.

**Version**: 1.0.0 | **Ratified**: 2026-04-10 | **Last Amended**: 2026-04-10
# Implementation Plan: crm-small Phase 1 — Foundation

**Branch**: `main` | **Date**: 2026-04-10 | **Spec**: PRD.md
**Input**: Feature specification from `PRD.md`

## Summary

crm-small is a B2B CRM REST API for IT services companies. It tracks Companies,
Contacts, Contracts, Invoices, and Interactions, and runs a nightly analytics
pipeline (RFM → LTV → BCG) to surface customer intelligence. Phase 1 is fully
implemented: the data model, REST API, and analytics scheduler are in place.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.2, Spring Data JPA, Hibernate,
  PostgreSQL JDBC driver, Flyway, JUnit 5, Testcontainers
**Storage**: PostgreSQL 14+ (port 5433 by default)
**Testing**: JUnit 5 + Testcontainers (real PostgreSQL, no mocks)
**Target Platform**: Linux/Windows server, JVM
**Project Type**: web-service (REST API + static HTML UI in `ui/`)
**Performance Goals**: Analytics pipeline completes within a nightly window;
  no strict latency SLA for Phase 1
**Constraints**: No Spring Security yet (Phase 4); single-tenant;
  configuration via `application.properties` only
**Scale/Scope**: Small-to-mid PME portfolio (dozens to low hundreds of clients)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I. Domain-Driven Aggregates | 5 root aggregates; UUID PKs; status transitions driven by domain events | ✅ Company/Contact/Contract/Invoice/Interaction; UUIDs via `gen_random_uuid()`; status auto-transitioned on paid event (PROSPECT→ACTIVE) and RFM degradation (ACTIVE→AT_RISK) |
| II. Analytics Pipeline Integrity | Fixed RFM→LTV→BCG order; portfolio-relative thresholds | ✅ `AnalyticsScheduler` calls rfm → ltv → bcg in order; NTILE(5) used; BCG uses portfolio medians |
| III. Test-First with Real Infrastructure | Testcontainers; no DB mocks | ✅ All three analytics service tests use Testcontainers + real PostgreSQL |
| IV. API Consistency | All responses in `ApiResponse<T>` | ✅ All controller methods return `ApiResponse<T>`; 200/201/204 used correctly |
| V. Operational Simplicity | `application.properties` only; Flyway auto-migrations | ✅ V1–V6 Flyway migrations; all config in `application.properties` |

**Result**: All gates pass. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/main/
├── plan.md              # This file
├── research.md          # Phase 0 — architecture decisions
├── data-model.md        # Phase 1 — entity design
├── quickstart.md        # Phase 1 — local dev guide
└── contracts/           # Phase 1 — API contracts per aggregate
    ├── companies.md
    ├── contracts-api.md
    ├── invoices.md
    ├── analytics.md
    └── interactions.md
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/com/crm/
│   │   ├── CrmApplication.java
│   │   ├── api/
│   │   │   ├── analytics/AnalyticsController.java
│   │   │   ├── company/CompanyController.java
│   │   │   ├── contract/ContractController.java
│   │   │   ├── invoice/InvoiceController.java
│   │   │   └── shared/
│   │   │       ├── ApiResponse.java
│   │   │       └── GlobalExceptionHandler.java
│   │   ├── domain/
│   │   │   ├── analytics/   (CompanyRfmScore, CompanyLtvRecord, CompanyBcgRecord,
│   │   │   │                  RfmSegment, BcgQuadrant, ChurnRisk)
│   │   │   ├── company/     (Company, CompanyStatus, CompanyTier)
│   │   │   ├── contact/     (Contact)
│   │   │   ├── contract/    (Contract, ContractPeriod, ContractPeriodStatus,
│   │   │   │                  ContractStatus, ServiceType)
│   │   │   ├── interaction/ (Interaction, InteractionType)
│   │   │   └── invoice/     (Invoice, InvoiceLineItem, InvoiceStatus)
│   │   ├── repository/      (one Spring Data JPA repo per aggregate root)
│   │   └── service/
│   │       ├── analytics/   (RfmCalculationService, LtvCalculationService,
│   │       │                  BcgCalculationService, AnalyticsScheduler)
│   │       ├── company/     (CompanyService)
│   │       ├── contract/    (ContractService)
│   │       ├── interaction/ (InteractionService)
│   │       └── invoice/     (InvoiceService)
│   └── resources/
│       ├── application.properties
│       └── db/migration/    (V1–V6 Flyway scripts)
└── test/
    └── java/com/crm/
        └── service/analytics/
            ├── RfmCalculationServiceTest.java
            ├── LtvCalculationServiceTest.java
            └── BcgCalculationServiceTest.java

ui/
├── dashboard.html
├── companies.html
├── company-detail.html
└── analytics-rfm.html
```

**Structure Decision**: Single Spring Boot project. REST API serves JSON;
UI is static HTML in `ui/` calling the API directly. No build pipeline for
the frontend; no module separation needed at current scale.

## Complexity Tracking

> No constitution violations — table not required.

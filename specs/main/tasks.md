# Tasks: crm-small Phase 1 — Foundation

**Input**: Design documents from `specs/main/`
**Prerequisites**: plan.md, PRD.md, research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.
**Tests**: Not included (not requested). Constitution-required analytics tests (RfmCalculationServiceTest, LtvCalculationServiceTest, BcgCalculationServiceTest) are listed in the Analytics phase.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US6)
- All paths are relative to repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Maven project skeleton, entry point, and runtime configuration

- [X] T001 Initialize Maven project with `pom.xml` declaring Java 21, Spring Boot 3.2, Spring Data JPA, Hibernate, PostgreSQL JDBC driver, Flyway, JUnit 5, Testcontainers at repository root
- [X] T002 [P] Create `src/main/java/com/crm/CrmApplication.java` — `@SpringBootApplication` entry point with `main()` method
- [X] T003 [P] Create `src/main/resources/application.properties` with all values from PRD §6: `spring.datasource.*` (port 5433), `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.enabled=true`, `crm.ltv.*` projection params, `crm.analytics.cron=0 0 2 * * *`

**Checkpoint**: `mvn compile` succeeds; application starts (DB not required yet)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared API envelope, exception handling, and all Flyway DDL migrations that every user story depends on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Create `src/main/java/com/crm/api/shared/ApiResponse.java` — generic record `ApiResponse<T>` with fields `boolean success`, `T data`, `String error` and static factory methods `ok(T data)` / `error(String msg)`
- [X] T005 [P] Create `src/main/java/com/crm/api/shared/GlobalExceptionHandler.java` — `@RestControllerAdvice` handling `EntityNotFoundException` → 404, `IllegalStateException` → 409, `MethodArgumentNotValidException` → 400, all returning `ApiResponse<Void>`
- [X] T006 Create `src/main/resources/db/migration/V1__create_company_contact.sql` — DDL for `company` table (UUID PK via `gen_random_uuid()`, name, industry, region, tier CHECK, status CHECK, acquired_date, churned_date, created_at DEFAULT NOW(), updated_at DEFAULT NOW()) and `contact` table (UUID PK, company_id FK ON DELETE CASCADE, first_name, last_name, email UNIQUE, phone, role, is_primary, created_at)
- [X] T007 [P] Create `src/main/resources/db/migration/V2__create_contract.sql` — DDL for `contract` table (UUID PK, company_id FK, service_type CHECK, monthly_value NUMERIC(15,2), start_date, end_date nullable, status CHECK, auto_renews DEFAULT FALSE, created_at) and `contract_period` table (UUID PK, contract_id FK, company_id FK denormalized, period_start, period_end, amount_billed NUMERIC(15,2), status CHECK, paid_at nullable)
- [X] T008 [P] Create `src/main/resources/db/migration/V3__create_invoice.sql` — DDL for `invoice` table (UUID PK, company_id FK, invoice_number VARCHAR(50) UNIQUE, issue_date, due_date, total_amount NUMERIC(15,2) DEFAULT 0, status CHECK, paid_date nullable, notes TEXT, created_at) and `invoice_line_item` table (UUID PK, invoice_id FK ON DELETE CASCADE, description, quantity NUMERIC(10,2), unit_price NUMERIC(15,2), line_total NUMERIC(15,2), service_category)
- [X] T009 [P] Create `src/main/resources/db/migration/V4__create_interaction.sql` — DDL for `interaction` table (UUID PK, company_id FK, contact_id FK nullable, interaction_type CHECK, interacted_at TIMESTAMP, notes TEXT, outcome VARCHAR(255), created_by VARCHAR(100), created_at)
- [X] T010 [P] Create `src/main/resources/db/migration/V5__create_analytics_tables.sql` — DDL for `company_rfm_score` (UUID PK, company_id FK UNIQUE, recency_days INT, recency_score SMALLINT, frequency_count INT, frequency_score SMALLINT, monetary_total NUMERIC(15,2), monetary_score SMALLINT, rfm_score SMALLINT, segment VARCHAR(30), calculated_at), `company_ltv_record` (UUID PK, company_id UNIQUE, historical_revenue, projected_annual_value, projected_ltv, avg_monthly_revenue NUMERIC(15,2), estimated_churn_risk VARCHAR(10), contract_months_remaining INT, calculated_at), `company_bcg_record` (UUID PK, company_id UNIQUE, current_year_revenue, previous_year_revenue, revenue_share_pct NUMERIC(8,4), revenue_growth_rate_pct NUMERIC(8,4), quadrant VARCHAR(20), calculated_at)
- [X] T011 Create `src/main/resources/db/migration/V6__create_indexes.sql` — indexes on `company(status)`, `contract(company_id)`, `contract_period(company_id, status)`, `invoice(company_id, status)`, `interaction(company_id, interacted_at DESC)`, `company_rfm_score(company_id)`, `company_ltv_record(company_id)`, `company_bcg_record(company_id)`

**Checkpoint**: Foundation ready — Flyway applies V1–V6 cleanly; `spring.jpa.hibernate.ddl-auto=validate` passes; user stories can now be implemented

---

## Phase 3: User Story 1 — Company & Contact Management (Priority: P1) 🎯 MVP

**Goal**: Account managers can create, read, update, and delete company records; track status transitions (PROSPECT → ACTIVE → AT_RISK → CHURNED); manage multiple contacts per company with one primary flag

**Independent Test**: `POST /api/companies` creates a PROSPECT company → `POST /api/companies/{id}/churn` returns 200 and status becomes CHURNED → `DELETE /api/companies/{id}` cascades and deletes contacts → `GET /api/companies?status=ACTIVE` returns filtered list

### Implementation for User Story 1

- [X] T012 [P] [US1] Create `src/main/java/com/crm/domain/company/CompanyStatus.java` — enum `PROSPECT, ACTIVE, AT_RISK, CHURNED`
- [X] T013 [P] [US1] Create `src/main/java/com/crm/domain/company/CompanyTier.java` — enum `SMB, MID_MARKET, ENTERPRISE`
- [X] T014 [US1] Create `src/main/java/com/crm/domain/company/Company.java` — `@Entity("company")` with all columns from data-model.md; `@Enumerated(EnumType.STRING)` for tier/status; `@PreUpdate` sets `updatedAt = now()`; `@OneToMany(cascade = CascadeType.ALL)` for contacts and interactions
- [X] T015 [US1] Create `src/main/java/com/crm/domain/contact/Contact.java` — `@Entity("contact")` with UUID PK, `@ManyToOne` to Company, all fields per data-model.md
- [X] T016 [P] [US1] Create `src/main/java/com/crm/repository/CompanyRepository.java` — `JpaRepository<Company, UUID>` with `List<Company> findByStatus(CompanyStatus status)`
- [X] T017 [P] [US1] Create `src/main/java/com/crm/repository/ContactRepository.java` — `JpaRepository<Contact, UUID>` with `List<Contact> findByCompanyId(UUID companyId)`
- [X] T018 [US1] Implement `src/main/java/com/crm/service/company/CompanyService.java` — `@Service` with `@Transactional` methods: `create(request)`, `findAll(Optional<CompanyStatus>)`, `findById(UUID)` (throws EntityNotFoundException), `update(UUID, request)`, `churn(UUID)` (sets status=CHURNED + churnedDate=today), `delete(UUID)`; all contact operations delegated to ContactRepository
- [X] T019 [US1] Implement `src/main/java/com/crm/api/company/CompanyController.java` — `@RestController @RequestMapping("/api/companies")` with `CompanyDto` inner record; endpoints: `POST /` → 201, `GET /` with `?status=` → 200, `GET /{id}` → 200/404, `PUT /{id}` → 200/404, `POST /{id}/churn` → 200/404, `DELETE /{id}` → 204/404; add `POST /{id}/contacts`, `GET /{id}/contacts` for contact management; all responses wrapped in `ApiResponse<T>`

**Checkpoint**: Company CRUD + churn + contact management fully functional via REST API independently of all other user stories

---

## Phase 4: User Story 2 — Contract & Billing Management (Priority: P2)

**Goal**: Account managers can create contracts for companies, record individual billing periods, and mark periods as paid — feeding the analytics pipeline with billing events

**Independent Test**: `POST /api/contracts` creates a contract → `POST /api/contracts/{id}/periods` adds a billing period in PENDING status → `POST /api/contracts/periods/{periodId}/pay` transitions to PAID (409 if already PAID) → `GET /api/contracts/{id}/periods` returns the paid period

### Implementation for User Story 2

- [X] T020 [P] [US2] Create `src/main/java/com/crm/domain/contract/ContractStatus.java` — enum `ACTIVE, EXPIRED, CANCELLED, PENDING_RENEWAL`
- [X] T021 [P] [US2] Create `src/main/java/com/crm/domain/contract/ContractPeriodStatus.java` — enum `PENDING, PAID, OVERDUE`
- [X] T022 [P] [US2] Create `src/main/java/com/crm/domain/contract/ServiceType.java` — enum `MANAGED_SERVICES, SUPPORT_RETAINER, SAAS_LICENSE, CONSULTING_RETAINER`
- [X] T023 [US2] Create `src/main/java/com/crm/domain/contract/Contract.java` — `@Entity("contract")` with all fields per data-model.md; `@ManyToOne` to Company; `@OneToMany` to ContractPeriod
- [X] T024 [US2] Create `src/main/java/com/crm/domain/contract/ContractPeriod.java` — `@Entity("contract_period")` with contractId FK, companyId denormalized FK, periodStart, periodEnd, amountBilled, status (default PENDING), paidAt nullable; `markPaid()` throws `IllegalStateException` if already PAID (→ 409)
- [X] T025 [P] [US2] Create `src/main/java/com/crm/repository/ContractRepository.java` — `JpaRepository<Contract, UUID>` with `List<Contract> findByCompanyId(UUID)` and `@Query` for contracts expiring within 60 days
- [X] T026 [P] [US2] Create `src/main/java/com/crm/repository/ContractPeriodRepository.java` — `JpaRepository<ContractPeriod, UUID>` with `List<ContractPeriod> findByContractId(UUID)` and `findByCompanyIdAndStatus(UUID, ContractPeriodStatus)`
- [X] T027 [US2] Implement `src/main/java/com/crm/service/contract/ContractService.java` — `@Service @Transactional` with: `create(request)` validates companyId exists, `findById(UUID)`, `findByCompanyId(UUID)`, `addPeriod(UUID contractId, request)` defaults status=PENDING, `markPeriodPaid(UUID periodId)` throws `IllegalStateException` if already PAID (→ 409), sets paidAt=now(), `listPeriods(UUID contractId)`
- [X] T028 [US2] Implement `src/main/java/com/crm/api/contract/ContractController.java` — `@RestController @RequestMapping("/api/contracts")` with `ContractDto` and `ContractPeriodDto` inner records; endpoints per contracts-api.md contract: `POST /`, `GET /company/{companyId}`, `GET /{id}`, `POST /{id}/periods` → 201, `POST /periods/{periodId}/pay` → 200/404/409, `GET /{id}/periods`

**Checkpoint**: Full contract lifecycle (create → add periods → mark paid) works independently; a PROSPECT company gains a billing event that will trigger ACTIVE promotion on next RFM run

---

## Phase 5: User Story 3 — Invoice Management (Priority: P3)

**Goal**: Account managers can create standalone invoices for companies, add line items, and mark invoices as paid — providing an alternative billing event source for analytics

**Independent Test**: `POST /api/invoices` creates DRAFT invoice → 409 on duplicate `invoiceNumber` → `POST /api/invoices/{id}/line-items` adds item and auto-updates `totalAmount` → `POST /api/invoices/{id}/pay` sets PAID (409 if already PAID/CANCELLED) → `GET /api/invoices/company/{id}` returns all invoices for company

### Implementation for User Story 3

- [X] T029 [P] [US3] Create `src/main/java/com/crm/domain/invoice/InvoiceStatus.java` — enum `DRAFT, SENT, PAID, OVERDUE, CANCELLED`
- [X] T030 [US3] Create `src/main/java/com/crm/domain/invoice/Invoice.java` — `@Entity("invoice")` with all fields per data-model.md; `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` to InvoiceLineItem; `invoiceNumber` annotated `@Column(unique = true)`; `markPaid()` throws `IllegalStateException` if PAID or CANCELLED (→ 409)
- [X] T031 [US3] Create `src/main/java/com/crm/domain/invoice/InvoiceLineItem.java` — `@Entity("invoice_line_item")` with `invoiceId FK`, description, quantity, unitPrice, `lineTotal` (computed as quantity × unitPrice in service before save), serviceCategory nullable
- [X] T032 [P] [US3] Create `src/main/java/com/crm/repository/InvoiceRepository.java` — `JpaRepository<Invoice, UUID>` with `findByCompanyId(UUID)` and `@Query` for invoices with dueDate more than 30 days past today and status != PAID/CANCELLED
- [X] T033 [P] [US3] InvoiceLineItem persistence handled via JPA cascade on Invoice entity — no separate repository needed; line items saved through `Invoice.addLineItem()` + `InvoiceRepository.save()`
- [X] T034 [US3] Implement `src/main/java/com/crm/service/invoice/InvoiceService.java` — `@Service @Transactional`: `create(request)` starts in DRAFT; `addLineItem(UUID invoiceId, request)` computes `lineTotal = quantity × unitPrice`, saves item, recomputes `invoice.totalAmount`; `markPaid(UUID id, LocalDate paidDate)` throws `IllegalStateException` if PAID or CANCELLED; `findById(UUID)`, `findByCompanyId(UUID)`
- [X] T035 [US3] Implement `src/main/java/com/crm/api/invoice/InvoiceController.java` — `@RestController @RequestMapping("/api/invoices")` with `InvoiceDto` and `InvoiceLineItemDto` inner records per invoices.md contract shape; endpoints: `POST /` → 201/409, `POST /{id}/line-items` → 201, `POST /{id}/pay?paidDate=` → 200/404/409, `GET /company/{companyId}` → 200, `GET /{id}` → 200/404 (includes lineItems list in response)

**Checkpoint**: Full invoice lifecycle works independently; paid invoices provide monetary data for analytics pipeline

---

## Phase 6: User Story 4 — Interaction Logging (Priority: P4)

**Goal**: Account managers can log client interactions (calls, emails, meetings, support tickets, renewal discussions) against a company with an optional contact reference, CRM user identifier, notes, and outcome

**Independent Test**: `POST /api/companies/{id}/interactions` logs a CALL interaction with notes → `GET /api/companies/{id}/interactions` returns interactions ordered by `interactedAt` descending → interaction with optional `contactId` resolves correctly

### Implementation for User Story 4

- [X] T036 [P] [US4] Create `src/main/java/com/crm/domain/interaction/InteractionType.java` — enum `CALL, EMAIL, MEETING, SUPPORT_TICKET, RENEWAL_DISCUSSION`
- [X] T037 [US4] Create `src/main/java/com/crm/domain/interaction/Interaction.java` — `@Entity("interaction")` with UUID PK, companyId FK, contactId FK nullable (`@ManyToOne(optional = true)`), `@Enumerated(EnumType.STRING) InteractionType`, interactedAt TIMESTAMP, notes TEXT, outcome VARCHAR(255), createdBy VARCHAR(100), createdAt
- [X] T038 [US4] Create `src/main/java/com/crm/repository/InteractionRepository.java` — `JpaRepository<Interaction, UUID>` with `List<Interaction> findByCompanyIdOrderByInteractedAtDesc(UUID companyId)`
- [X] T039 [US4] Implement `src/main/java/com/crm/service/interaction/InteractionService.java` — `@Service @Transactional`: `log(UUID companyId, request)` validates company exists (throws EntityNotFoundException), optionally validates contactId, saves interaction; `findByCompany(UUID companyId)` returns ordered list
- [X] T040 [US4] Add interaction endpoints to `src/main/java/com/crm/api/company/CompanyController.java` — `POST /api/companies/{id}/interactions` → 201 (creates interaction with `InteractionDto` inner record: interactionType, interactedAt, notes, outcome, createdBy, optional contactId); `GET /api/companies/{id}/interactions` → 200 (list ordered by interactedAt desc)

**Checkpoint**: Interaction logging fully functional; interactions visible in company context; all 5 interaction types accepted

---

## Phase 7: User Story 5 — Analytics Pipeline: RFM → LTV → BCG (Priority: P5)

**Goal**: Nightly scheduler (02:00 AM) and manual trigger compute portfolio-relative RFM scores using NTILE(5) window functions, derive LTV with churn-risk discounting, assign BCG quadrants using portfolio-median thresholds, auto-transition company statuses, and log renewal/overdue alerts

**Independent Test**: Insert one company with paid contract periods → `POST /api/analytics/recalculate` returns 200 → `GET /api/analytics/rfm/company/{id}` shows non-null RFM scores and segment → `GET /api/analytics/ltv/company/{id}` shows projected LTV → `GET /api/analytics/bcg/company/{id}` shows quadrant

### Implementation for User Story 5

- [X] T041 [P] [US5] Create `src/main/java/com/crm/domain/analytics/RfmSegment.java` — enum `CHAMPION, LOYAL, POTENTIAL_LOYALIST, NEW, AT_RISK, CANT_LOSE, HIBERNATING, LOST`; segment assignment logic in `RfmCalculationService.assignSegment()`
- [X] T042 [P] [US5] Create `src/main/java/com/crm/domain/analytics/BcgQuadrant.java` — enum `STAR, CASH_COW, QUESTION_MARK, DOG`
- [X] T043 [P] [US5] Create `src/main/java/com/crm/domain/analytics/ChurnRisk.java` — enum `LOW, MEDIUM, HIGH` with static `from(RfmSegment)` mapping in `LtvCalculationService.deriveChurnRisk()`
- [X] T044 [US5] Create `src/main/java/com/crm/domain/analytics/CompanyRfmScore.java` — `@Entity("company_rfm_score")` with all fields per data-model.md; `@Column(unique = true) UUID companyId`; `@Enumerated(EnumType.STRING) RfmSegment segment`
- [X] T045 [P] [US5] Create `src/main/java/com/crm/domain/analytics/CompanyLtvRecord.java` — `@Entity("company_ltv_record")` with all LTV fields per data-model.md; `@Enumerated(EnumType.STRING) ChurnRisk estimatedChurnRisk`
- [X] T046 [P] [US5] Create `src/main/java/com/crm/domain/analytics/CompanyBcgRecord.java` — `@Entity("company_bcg_record")` with all BCG fields per data-model.md; `@Enumerated(EnumType.STRING) BcgQuadrant quadrant`
- [X] T047 [P] [US5] Create `src/main/java/com/crm/repository/CompanyRfmScoreRepository.java` — `JpaRepository<CompanyRfmScore, UUID>` with `findByCompanyId(UUID)`, native NTILE(5) query, `findAtRiskHighValue()`, `countBySegment()`
- [X] T048 [P] [US5] Create `src/main/java/com/crm/repository/CompanyLtvRecordRepository.java` — `JpaRepository<CompanyLtvRecord, UUID>` with `findByCompanyId(UUID)` and `findTopByProjectedLtv(Pageable)`
- [X] T049 [P] [US5] Create `src/main/java/com/crm/repository/CompanyBcgRecordRepository.java` — `JpaRepository<CompanyBcgRecord, UUID>` with `findByCompanyId(UUID)`, `findByQuadrant(BcgQuadrant)`, `countByQuadrant()` native query
- [X] T050 [US5] Implement `src/main/java/com/crm/service/analytics/RfmCalculationService.java` — native SQL NTILE(5) over paid contract_period + invoice events; segment assignment; PROSPECT→ACTIVE and ACTIVE→AT_RISK status transitions; upsert to company_rfm_score
- [X] T051 [US5] Implement `src/main/java/com/crm/service/analytics/LtvCalculationService.java` — historicalRevenue, projectedAnnualValue, projectedFutureRevenue, churn discount via `@Value` properties; upsert to company_ltv_record; reads RFM segments
- [X] T052 [US5] Implement `src/main/java/com/crm/service/analytics/BcgCalculationService.java` — current/prev year revenue aggregation; portfolio-median thresholds; quadrant assignment; upsert to company_bcg_record
- [X] T053 [US5] Implement `src/main/java/com/crm/service/analytics/AnalyticsScheduler.java` — `@Scheduled(cron = "${crm.analytics.cron}")` calls RFM→LTV→BCG in order; renewal alerts (60 days); overdue invoice alerts (30 days)
- [X] T054 [US5] Implement `src/main/java/com/crm/api/analytics/AnalyticsController.java` — all endpoints per PRD §7: RFM, LTV, BCG reads + `POST /recalculate`; all in `ApiResponse<T>`

**Checkpoint**: Full analytics pipeline executes end-to-end: paid billing events produce RFM scores, LTV records, BCG quadrants, company status auto-transitions, renewal/overdue alerts logged

---

## Phase 8: User Story 6 — Static HTML Dashboard & UI (Priority: P6)

**Goal**: Account managers and sales managers can view portfolio health, company lists, company detail with analytics, and RFM analysis through static HTML pages that fetch data directly from the REST API

**Independent Test**: Open each HTML page in browser → KPI cards and charts populate from `/api/analytics/*` and `/api/companies` → Company detail tabs load per-company data → RFM heatmap renders correct 5×5 grid

### Implementation for User Story 6

- [X] T055 [P] [US6] Create `ui/dashboard.html` — 4 KPI cards; Customer Segments donut chart; BCG Portfolio 2×2 grid; Top Customers by LTV; Recent Activity feed
- [X] T056 [P] [US6] Create `ui/companies.html` — sortable filterable table with filter pills and client-side search
- [X] T057 [P] [US6] Create `ui/company-detail.html` — 5-tab layout (Profile/Analytics/Contracts/Invoices/Interactions)
- [X] T058 [P] [US6] Create `ui/analytics-rfm.html` — 8 segment summary cards; 5×5 R×F heatmap; Monetary Score Distribution; Segment Movements panel

**Checkpoint**: All 4 HTML pages render portfolio data in browser without backend code changes

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: CORS, configuration validation, end-to-end smoke test

- [X] T059 [P] Add CORS configuration — `src/main/java/com/crm/WebConfig.java` implementing `WebMvcConfigurer.addCorsMappings()` to allow all origins on `/api/**` (no auth in Phase 1)
- [ ] T060 Smoke-test end-to-end pipeline: verify all 6 Flyway migrations apply in order on fresh PostgreSQL 14+ instance, application starts with `ddl-auto=validate` passing, `POST /api/analytics/recalculate` completes RFM→LTV→BCG run without error, result records visible via `GET /api/analytics/rfm`, `GET /api/analytics/ltv/top`, `GET /api/analytics/bcg`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — **BLOCKS all user stories**
- **User Stories (Phases 3–8)**: All depend on Foundational (Phase 2) completion
  - US1–US4 are independent of each other and can proceed in parallel after Phase 2
  - US5 (Analytics) depends on US1 + US2 + US3 for billing event data; implement last among core stories
  - US6 (UI) depends on US1–US5 APIs being available; can be implemented in parallel once endpoints exist
- **Polish (Phase 9)**: Depends on all user story phases

### User Story Dependencies

| Story | Depends On | Independent? |
|-------|-----------|-------------|
| US1 Company & Contact (P1) | Phase 2 only | ✅ Yes |
| US2 Contract & Billing (P2) | Phase 2 + US1 (companyId FK) | Mostly (needs Company entity) |
| US3 Invoice Management (P3) | Phase 2 + US1 (companyId FK) | Mostly (needs Company entity) |
| US4 Interaction Logging (P4) | Phase 2 + US1 (companyId FK) | Mostly (needs Company entity) |
| US5 Analytics Pipeline (P5) | Phase 2 + US1 + US2 + US3 | No — reads billing data |
| US6 Static HTML UI (P6) | All APIs (US1–US5) | No — needs all endpoints |

### Within Each User Story

- Enums → Entity → Repository → Service → Controller (sequential)
- Parallel [P] tasks within a phase (enums, repos) can run simultaneously
- Complete each story before moving to lower-priority stories for clean testability

### Parallel Opportunities

- Phase 1: T002 and T003 in parallel
- Phase 2: T005, T007, T008, T009, T010 in parallel after T004 and T006
- Phase 3: T012 + T013 in parallel; T016 + T017 in parallel
- Phase 4: T020 + T021 + T022 in parallel; T025 + T026 in parallel
- Phase 5: T029 in parallel with entity setup
- Phase 6: T036 in parallel with T037 setup
- Phase 7: T041 + T042 + T043 in parallel; T044 → then T045 + T046 in parallel; T047 + T048 + T049 in parallel
- Phase 8: T055 + T056 + T057 + T058 all in parallel

---

## Parallel Example: User Story 5 (Analytics)

```
# Step 1 — Enums (fully parallel):
Task T041: Create RfmSegment enum
Task T042: Create BcgQuadrant enum
Task T043: Create ChurnRisk enum

# Step 2 — Entities (T044 first, then T045+T046 in parallel):
Task T044: Create CompanyRfmScore entity
→ Task T045: Create CompanyLtvRecord entity
→ Task T046: Create CompanyBcgRecord entity

# Step 3 — Repositories (parallel after entities):
Task T047: Create CompanyRfmScoreRepository
Task T048: Create CompanyLtvRecordRepository
Task T049: Create CompanyBcgRecordRepository

# Step 4 — Services (sequential, RFM→LTV→BCG order matters):
Task T050: Implement RfmCalculationService
→ Task T051: Implement LtvCalculationService (reads RFM segments)
→ Task T052: Implement BcgCalculationService
→ Task T053: Implement AnalyticsScheduler (orchestrates all three)
→ Task T054: Implement AnalyticsController
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (Company & Contact Management)
4. **STOP and VALIDATE**: `POST /api/companies` → `GET /api/companies` → `DELETE /api/companies/{id}`
5. Demo or deploy minimal CRM shell

### Incremental Delivery

1. Setup + Foundational → DB schema and shared infra ready
2. US1 (P1) → Company/Contact CRUD → first deliverable
3. US2 (P2) → Contract/billing → CRM usable for account tracking
4. US3 (P3) → Invoices → Full billing picture
5. US4 (P4) → Interactions → Relationship history captured
6. US5 (P5) → Analytics → Portfolio intelligence surfaced
7. US6 (P6) → HTML UI → Self-service dashboard for managers
8. Polish → Production-ready

### Parallel Team Strategy

After Phase 2 completes:
- **Developer A**: US1 (Company/Contact) + US4 (Interactions)
- **Developer B**: US2 (Contracts) + US3 (Invoices)
- **Developer C**: US5 (Analytics) — starts after US1/US2/US3 entities exist
- **Developer D**: US6 (HTML UI) — starts once API endpoints are available

---

## Notes

- `[P]` tasks = independent files, no dependency conflicts — launch simultaneously
- `[USn]` label maps each task to its user story for traceability
- No mocking: integration tests must use Testcontainers (Principle III from constitution)
- Analytics native SQL must use PostgreSQL-specific NTILE(5) window functions — H2 not compatible
- All REST responses wrapped in `ApiResponse<T>` (Principle IV)
- Schema changes go through Flyway only — never via `ddl-auto=create` or `update` (Principle V)
- No Spring Security in Phase 1 — authentication deferred to Phase 4

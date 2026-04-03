# Product Requirements Document — crm-small

## 1. Overview

**crm-small** is a B2B CRM platform designed for IT services companies managing a portfolio of SMB, mid-market, and enterprise clients. It centralises contract and invoice tracking alongside three analytical frameworks — RFM, LTV, and BCG Matrix — to help account managers prioritise retention efforts and understand portfolio health.

**Core value proposition:** Turn billing and interaction history into actionable customer intelligence, surfaced through a dashboard and analytics screens without requiring manual reporting.

---

## 2. User Personas

| Persona | Role | Primary Goals |
|---|---|---|
| Account Manager | Day-to-day management of client relationships | Track contract renewals, log interactions, monitor at-risk clients |
| Sales Manager | Portfolio oversight and revenue forecasting | Identify high-LTV targets, analyse BCG quadrant distribution, act on RFM segments |

---

## 3. Functional Requirements

### 3.1 Company Management

- Create, read, update, and delete company records.
- Each company has: name, industry, region, tier (SMB / MID_MARKET / ENTERPRISE), status, and acquisition date.
- **Statuses:** PROSPECT → ACTIVE → AT_RISK → CHURNED.
  - A company is auto-promoted from PROSPECT to ACTIVE on its first paid billing event.
  - A company is auto-flagged as AT_RISK when its RFM segment degrades to AT_RISK, HIBERNATING, CANT_LOSE, or LOST.
- Churning a company records the churn date and sets status to CHURNED.
- Companies can be filtered by status via the API.

### 3.2 Contact Management

- Each company can have multiple contacts (first name, last name, email, phone, role).
- One contact per company can be flagged as primary.
- Contacts are deleted when their parent company is deleted (cascade).

### 3.3 Contract & Contract Period Management

- A company can have multiple contracts.
- **Service types:** MANAGED_SERVICES, SUPPORT_RETAINER, SAAS_LICENSE, CONSULTING_RETAINER.
- Each contract has a monthly value, start date, optional end date, and status (ACTIVE, EXPIRED, CANCELLED, PENDING_RENEWAL).
- Contracts support an auto-renews flag.
- **Contract Periods** represent individual billing cycles within a contract.
  - Fields: period start/end, amount billed, status (PENDING / PAID / OVERDUE).
  - Marking a period as paid records the payment timestamp and feeds analytics.
- The scheduler alerts on contracts expiring within 60 days.

### 3.4 Invoice Management

- Invoices are standalone billing documents attached to a company.
- Each invoice has: invoice number (unique), issue date, due date, status (DRAFT, SENT, PAID, OVERDUE, CANCELLED), and notes.
- **Line items** compose the invoice total: description, quantity, unit price, line total (auto-calculated), and optional service category.
- Marking an invoice as paid accepts an optional paid date (defaults to today).
- The scheduler alerts on invoices overdue by more than 30 days.

### 3.5 Interaction Logging

- Log client interactions against a company, with an optional contact reference.
- **Interaction types:** CALL, EMAIL, MEETING, SUPPORT_TICKET, RENEWAL_DISCUSSION.
- Each interaction records: timestamp, notes, outcome, and the CRM user who created it.

---

### 3.6 Analytics — RFM Scoring

RFM scores are calculated across all companies that have at least one paid billing event (contract period or invoice).

| Dimension | Definition | Scoring |
|---|---|---|
| Recency | Days since last paid event | NTILE(5), inverted (lower days = score 5) |
| Frequency | Count of paid events | NTILE(5) (higher count = score 5) |
| Monetary | Sum of all paid amounts | NTILE(5) (higher amount = score 5) |

- Composite RFM score: `R × 100 + F × 10 + M`.
- Scores are portfolio-relative (percentile-based), not fixed thresholds.

**Segments (8):**

| Segment | Behaviour |
|---|---|
| CHAMPION | Best customers — recent, frequent, high spend |
| LOYAL | Regular buyers with solid monetary value |
| POTENTIAL_LOYALIST | Recent customers with growing frequency |
| NEW | Very recent, low frequency |
| AT_RISK | Previously good customers going quiet |
| CANT_LOSE | High monetary value but disengaging |
| HIBERNATING | Low recency and frequency |
| LOST | Lowest scores across all dimensions |

- The analytics screen provides segment summary cards (count + avg LTV) and an R×F score heatmap (5×5 grid showing company count and avg LTV per cell).

### 3.7 Analytics — LTV (Lifetime Value)

LTV is calculated per company after RFM scores are available (run order: RFM first).

**Revenue components:**

| Component | Calculation |
|---|---|
| Historical Revenue | Sum of all paid contract periods + paid invoices |
| Projected Annual Value | Sum of (monthlyValue × 12) for all active contracts |
| Projected Future Revenue | Avg monthly project revenue × 24 months × 0.7 discount factor |
| Raw LTV | Historical + Projected Annual + Projected Future |
| Churn Risk Discount | Applied to Raw LTV based on RFM segment |

**Churn risk mapping:**

| Risk Level | RFM Segments | Discount |
|---|---|---|
| LOW | CHAMPION, LOYAL, POTENTIAL_LOYALIST, NEW | 0% |
| MEDIUM | AT_RISK, HIBERNATING | 15% |
| HIGH | CANT_LOSE, LOST | 35% |

**Projected LTV** = Raw LTV × (1 − churn discount).

Stored fields: historical revenue, projected annual value, projected LTV, avg monthly revenue, estimated churn risk, and contract months remaining.

### 3.8 Analytics — BCG Matrix

BCG quadrant assignment is portfolio-relative (median-based, not fixed thresholds).

| Axis | Definition |
|---|---|
| Revenue Share (X) | Company current-year revenue ÷ total portfolio current-year revenue × 100% |
| Revenue Growth (Y) | (Current year − previous year) ÷ previous year × 100% |

- New customers with no prior-year revenue are assigned 100% growth.
- Thresholds = portfolio medians for each axis.

**Quadrants:**

| Quadrant | Share | Growth |
|---|---|---|
| STAR | ≥ median | ≥ median |
| CASH_COW | ≥ median | < median |
| QUESTION_MARK | < median | ≥ median |
| DOG | < median | < median |

### 3.9 Scheduled Jobs

Runs nightly at **02:00 AM** (configurable).

Execution order:
1. RFM recalculation
2. LTV recalculation (reads RFM segments)
3. BCG recalculation

Additional checks per run:
- **Renewal alerts** — log warning for contracts expiring within 60 days.
- **Overdue invoice alerts** — log warning for invoices overdue by more than 30 days.

A `POST /api/analytics/recalculate` endpoint allows manual triggering of the full pipeline.

---

## 4. UI Screens

### 4.1 Dashboard
- 4 KPI cards: Total Portfolio LTV, Active Contracts, Outstanding Invoices, At-Risk Companies.
- Customer Segments donut chart showing distribution across all 8 RFM segments.
- BCG Portfolio Overview: 2×2 grid with revenue totals per quadrant.
- Top Customers by LTV: ranked list with RFM and BCG badges.
- Recent Activity feed: latest invoice payments, contract events, and status changes.

### 4.2 Companies List
- Sortable, filterable table with columns: Company, Status, RFM Segment, BCG Quadrant, LTV, Last Contact, Actions.
- Filter pills: by RFM segment, BCG quadrant, and status.
- Search by name or industry.
- Paginated results.

### 4.3 Company Detail
Five tabs per company:

| Tab | Contents |
|---|---|
| Profile | Contact info, industry, tier, acquisition date, primary contact card, tags, notes |
| Analytics | LTV, RFM scores (R/F/M with progress bars + segment badge), BCG position on 2×2 matrix |
| Contracts | List of contracts with status, monthly value, dates |
| Invoices | List of invoices with status, amount, dates |
| Interactions | Interaction log with type, date, notes, outcome |

### 4.4 RFM Analysis
- Segment summary cards (8 segments): client count and avg LTV.
- R×F Score Heatmap: 5×5 table with company count and avg LTV at each Recency × Frequency combination.
- Monetary Score Distribution bar chart.
- Segment Movements panel: quarter-over-quarter migrations between segments.
- Last calculated timestamp displayed in the header.

---

## 5. Non-Functional Requirements

| Requirement | Detail |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 14+ |
| Migrations | Flyway (applied automatically on startup) |
| Primary Keys | UUID (`gen_random_uuid()`) on all tables |
| API format | JSON, all responses wrapped in `ApiResponse<T>` |
| Analytics queries | Native SQL with window functions (NTILE) run server-side |
| Tests | JUnit 5 + Testcontainers (real PostgreSQL, no mocks) |
| Build | Maven |

---

## 6. Configuration Reference

All values are set in `src/main/resources/application.properties`.

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/crm_small` | Database connection URL |
| `spring.datasource.username` | `postgres` | DB username |
| `spring.datasource.password` | `postgres` | DB password |
| `crm.ltv.projection-months` | `24` | Months of future revenue projected in LTV |
| `crm.ltv.project-revenue-discount-factor` | `0.7` | Discount applied to non-contracted projected revenue |
| `crm.ltv.churn-risk-discount.medium` | `0.15` | LTV discount for MEDIUM churn risk |
| `crm.ltv.churn-risk-discount.high` | `0.35` | LTV discount for HIGH churn risk |
| `crm.analytics.cron` | `0 0 2 * * *` | Cron expression for nightly analytics run |

---

## 7. API Summary

### Companies
| Method | Path | Description |
|---|---|---|
| POST | `/api/companies` | Create company |
| GET | `/api/companies` | List all (optional `?status=` filter) |
| GET | `/api/companies/{id}` | Get company |
| PUT | `/api/companies/{id}` | Update company |
| POST | `/api/companies/{id}/churn` | Mark as churned |
| DELETE | `/api/companies/{id}` | Delete company |

### Contracts
| Method | Path | Description |
|---|---|---|
| POST | `/api/contracts` | Create contract |
| GET | `/api/contracts/company/{companyId}` | List contracts for company |
| GET | `/api/contracts/{id}` | Get contract |
| POST | `/api/contracts/{id}/periods` | Add contract period |
| POST | `/api/contracts/periods/{periodId}/pay` | Mark period as paid |
| GET | `/api/contracts/{id}/periods` | List periods for contract |

### Invoices
| Method | Path | Description |
|---|---|---|
| POST | `/api/invoices` | Create invoice |
| POST | `/api/invoices/{id}/line-items` | Add line item |
| POST | `/api/invoices/{id}/pay` | Mark as paid (optional `?paidDate=`) |
| GET | `/api/invoices/company/{companyId}` | List invoices for company |
| GET | `/api/invoices/{id}` | Get invoice |

### Analytics
| Method | Path | Description |
|---|---|---|
| GET | `/api/analytics/rfm` | All RFM scores |
| GET | `/api/analytics/rfm/company/{companyId}` | RFM score for company |
| GET | `/api/analytics/rfm/segments` | Count per segment |
| GET | `/api/analytics/rfm/at-risk` | At-risk companies with high monetary value |
| GET | `/api/analytics/ltv/top` | Top companies by LTV (`?limit=10`) |
| GET | `/api/analytics/ltv/company/{companyId}` | LTV record for company |
| GET | `/api/analytics/bcg` | All BCG records |
| GET | `/api/analytics/bcg/company/{companyId}` | BCG record for company |
| GET | `/api/analytics/bcg/quadrant/{quadrant}` | Companies in quadrant |
| GET | `/api/analytics/bcg/summary` | Count per quadrant |
| POST | `/api/analytics/recalculate` | Manually trigger full analytics pipeline |

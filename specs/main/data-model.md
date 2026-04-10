# Data Model: crm-small Phase 1

**Date**: 2026-04-10
**Flyway migrations**: V1–V6 in `src/main/resources/db/migration/`

## Entity Relationship Overview

```
Company (root)
  ├── Contact (1:N, cascade delete)
  ├── Contract (1:N)
  │     └── ContractPeriod (1:N)
  ├── Invoice (1:N)
  │     └── InvoiceLineItem (1:N, cascade delete)
  ├── Interaction (1:N)
  ├── CompanyRfmScore (1:1, upsert on recalculate)
  ├── CompanyLtvRecord (1:1, upsert on recalculate)
  └── CompanyBcgRecord (1:1, upsert on recalculate)
```

---

## Core Aggregates

### Company `company`

Root aggregate. All other entities reference `company.id`.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK, `gen_random_uuid()` | |
| `name` | VARCHAR(255) | NOT NULL | |
| `industry` | VARCHAR(100) | nullable | |
| `region` | VARCHAR(100) | nullable | |
| `tier` | VARCHAR(20) | NOT NULL, CHECK IN ('SMB','MID_MARKET','ENTERPRISE') | Default: `SMB` |
| `status` | VARCHAR(20) | NOT NULL, CHECK IN ('PROSPECT','ACTIVE','AT_RISK','CHURNED') | Default: `PROSPECT` |
| `acquired_date` | DATE | nullable | |
| `churned_date` | DATE | nullable | Set when POST /churn called |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Immutable |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Auto-updated via `@PreUpdate` |

**Status transitions** (domain-event-driven):
- `PROSPECT → ACTIVE`: triggered when first paid billing event (ContractPeriod
  or Invoice) is processed during RFM recalculation.
- `ACTIVE → AT_RISK`: triggered when RFM segment degrades to `AT_RISK` or
  `CANT_LOSE` during recalculation.
- `* → CHURNED`: triggered by explicit `POST /api/companies/{id}/churn`; sets
  `churned_date` to today.

---

### Contact `contact`

Each company can have multiple contacts. One contact per company can be
flagged `is_primary = true`.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK | |
| `company_id` | UUID | FK → company(id) ON DELETE CASCADE | |
| `first_name` | VARCHAR(100) | NOT NULL | |
| `last_name` | VARCHAR(100) | NOT NULL | |
| `email` | VARCHAR(255) | UNIQUE, nullable | |
| `phone` | VARCHAR(50) | nullable | |
| `role` | VARCHAR(100) | nullable | |
| `is_primary` | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

---

### Contract `contract`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK | |
| `company_id` | UUID | FK → company(id) | |
| `service_type` | VARCHAR(30) | CHECK IN ('MANAGED_SERVICES','SUPPORT_RETAINER','SAAS_LICENSE','CONSULTING_RETAINER') | |
| `monthly_value` | NUMERIC(15,2) | NOT NULL | |
| `start_date` | DATE | NOT NULL | |
| `end_date` | DATE | nullable | |
| `status` | VARCHAR(20) | CHECK IN ('ACTIVE','EXPIRED','CANCELLED','PENDING_RENEWAL') | |
| `auto_renews` | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

---

### ContractPeriod `contract_period`

Individual billing cycles within a contract. Paid periods feed the RFM
monetary and frequency calculations.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK | |
| `contract_id` | UUID | FK → contract(id) | |
| `company_id` | UUID | FK → company(id) | Denormalized for analytics queries |
| `period_start` | DATE | NOT NULL | |
| `period_end` | DATE | NOT NULL | |
| `amount_billed` | NUMERIC(15,2) | NOT NULL | |
| `status` | VARCHAR(10) | CHECK IN ('PENDING','PAID','OVERDUE') | Default: `PENDING` |
| `paid_at` | TIMESTAMP | nullable | Set when period is marked PAID |

---

### Invoice `invoice`

Standalone billing document attached to a company. Paid invoices feed RFM
analytics alongside paid contract periods.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK | |
| `company_id` | UUID | FK → company(id) | |
| `invoice_number` | VARCHAR(50) | NOT NULL, UNIQUE | |
| `issue_date` | DATE | NOT NULL | |
| `due_date` | DATE | NOT NULL | |
| `total_amount` | NUMERIC(15,2) | NOT NULL, DEFAULT 0 | Updated when line items change |
| `status` | VARCHAR(20) | CHECK IN ('DRAFT','SENT','PAID','OVERDUE','CANCELLED') | Default: `DRAFT` |
| `paid_date` | DATE | nullable | |
| `notes` | TEXT | nullable | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

---

### InvoiceLineItem `invoice_line_item`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK | |
| `invoice_id` | UUID | FK → invoice(id) ON DELETE CASCADE | |
| `description` | VARCHAR(255) | NOT NULL | |
| `quantity` | NUMERIC(10,2) | NOT NULL | |
| `unit_price` | NUMERIC(15,2) | NOT NULL | |
| `line_total` | NUMERIC(15,2) | NOT NULL | Auto-calculated: quantity × unit_price |
| `service_category` | VARCHAR(100) | nullable | |

---

### Interaction `interaction`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK | |
| `company_id` | UUID | FK → company(id) | |
| `contact_id` | UUID | FK → contact(id), nullable | Optional reference |
| `interaction_type` | VARCHAR(30) | CHECK IN ('CALL','EMAIL','MEETING','SUPPORT_TICKET','RENEWAL_DISCUSSION') | |
| `interacted_at` | TIMESTAMP | NOT NULL | |
| `notes` | TEXT | nullable | |
| `outcome` | VARCHAR(255) | nullable | |
| `created_by` | VARCHAR(100) | NOT NULL | CRM user identifier |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

---

## Analytics Tables

All analytics tables are upsert-on-recalculate: the service does a find-or-
create on `company_id`. A `calculated_at` timestamp is updated on every run.

### CompanyRfmScore `company_rfm_score`

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `company_id` | UUID | FK → company(id), UNIQUE |
| `recency_days` | INT | Days since last paid event |
| `recency_score` | SMALLINT | 1–5 (5 = most recent) |
| `frequency_count` | INT | Total paid events |
| `frequency_score` | SMALLINT | 1–5 (5 = most frequent) |
| `monetary_total` | NUMERIC(15,2) | Total paid amount |
| `monetary_score` | SMALLINT | 1–5 (5 = highest spend) |
| `rfm_score` | SMALLINT | `R×100 + F×10 + M` composite |
| `segment` | VARCHAR(30) | `RfmSegment` enum value |
| `calculated_at` | TIMESTAMP | Last recalculation timestamp |

**RFM Segment assignment rules** (first match wins):

| Segment | R | F | M |
|---------|---|---|---|
| CHAMPION | ≥4 | ≥4 | ≥4 |
| CANT_LOSE | ≤2 | ≥4 | ≥4 |
| AT_RISK | ≤2 | ≥3 | ≥3 |
| LOYAL | ≥3 | ≥4 | any |
| NEW | =5 | =1 | any |
| POTENTIAL_LOYALIST | ≥4 | ≤2 | any |
| HIBERNATING | ≤2 | ≤2 | any |
| LOST | =1 | =1 | any |
| *(default)* | — | — | — | LOYAL |

---

### CompanyLtvRecord `company_ltv_record`

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `company_id` | UUID | FK → company(id), UNIQUE |
| `historical_revenue` | NUMERIC(15,2) | Sum of all paid events |
| `projected_annual_value` | NUMERIC(15,2) | Sum(monthlyValue × 12) for ACTIVE contracts |
| `projected_ltv` | NUMERIC(15,2) | Raw LTV × (1 − churn discount) |
| `avg_monthly_revenue` | NUMERIC(15,2) | |
| `estimated_churn_risk` | VARCHAR(10) | LOW / MEDIUM / HIGH |
| `contract_months_remaining` | INT | Months until nearest contract end |
| `calculated_at` | TIMESTAMP | |

**Churn risk mapping**:

| Risk | Segments | Discount |
|------|----------|---------|
| LOW | CHAMPION, LOYAL, POTENTIAL_LOYALIST, NEW | 0% |
| MEDIUM | AT_RISK, HIBERNATING | 15% |
| HIGH | CANT_LOSE, LOST | 35% |

---

### CompanyBcgRecord `company_bcg_record`

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `company_id` | UUID | FK → company(id), UNIQUE |
| `current_year_revenue` | NUMERIC(15,2) | |
| `previous_year_revenue` | NUMERIC(15,2) | |
| `revenue_share_pct` | NUMERIC(8,4) | Company rev ÷ portfolio total rev × 100 |
| `revenue_growth_rate_pct` | NUMERIC(8,4) | YoY growth %; 100% for new customers |
| `quadrant` | VARCHAR(20) | STAR / CASH_COW / QUESTION_MARK / DOG |
| `calculated_at` | TIMESTAMP | |

**Quadrant assignment** (portfolio-median thresholds):

| Quadrant | Revenue Share | Revenue Growth |
|----------|--------------|----------------|
| STAR | ≥ median | ≥ median |
| CASH_COW | ≥ median | < median |
| QUESTION_MARK | < median | ≥ median |
| DOG | < median | < median |

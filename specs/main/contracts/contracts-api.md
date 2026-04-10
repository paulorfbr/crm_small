# API Contract: Contracts & Contract Periods

Base path: `/api/contracts`
All responses: `ApiResponse<T>` envelope.

---

## POST /api/contracts

Create a contract for a company.

**Request body**
```json
{
  "companyId": "550e8400-e29b-41d4-a716-446655440000",
  "serviceType": "MANAGED_SERVICES",
  "monthlyValue": 2500.00,
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "autoRenews": true
}
```

| Field | Type | Required | Values |
|-------|------|----------|--------|
| `companyId` | UUID | ✅ | existing company |
| `serviceType` | enum | ✅ | `MANAGED_SERVICES`, `SUPPORT_RETAINER`, `SAAS_LICENSE`, `CONSULTING_RETAINER` |
| `monthlyValue` | decimal | ✅ | > 0 |
| `startDate` | date (ISO) | ✅ | |
| `endDate` | date (ISO) | ✗ | null = open-ended |
| `autoRenews` | boolean | ✗ | default `false` |

**Response** `201 Created` — `ApiResponse<ContractDto>`

```json
{
  "success": true,
  "data": {
    "id": "...",
    "companyId": "...",
    "serviceType": "MANAGED_SERVICES",
    "monthlyValue": 2500.00,
    "startDate": "2026-01-01",
    "endDate": "2026-12-31",
    "status": "ACTIVE",
    "autoRenews": true
  }
}
```

---

## GET /api/contracts/company/{companyId}

List all contracts for a company.

**Response** `200 OK` — `ApiResponse<List<ContractDto>>`

---

## GET /api/contracts/{id}

Get a single contract.

**Response** `200 OK` — `ApiResponse<ContractDto>`
**Error**: `404 Not Found`

---

## POST /api/contracts/{id}/periods

Add a billing period to a contract.

**Request body**
```json
{
  "periodStart": "2026-01-01",
  "periodEnd": "2026-01-31",
  "amountBilled": 2500.00
}
```

**Response** `201 Created` — `ApiResponse<ContractPeriodDto>`

```json
{
  "success": true,
  "data": {
    "id": "...",
    "contractId": "...",
    "periodStart": "2026-01-01",
    "periodEnd": "2026-01-31",
    "amountBilled": 2500.00,
    "status": "PENDING",
    "paidAt": null
  }
}
```

---

## POST /api/contracts/periods/{periodId}/pay

Mark a contract period as paid. Records `paid_at = now()` and triggers
PROSPECT→ACTIVE status transition on the parent company (via next RFM run).

**Request body**: none
**Response** `200 OK` — `ApiResponse<ContractPeriodDto>` (updated record)
**Error**: `404 Not Found`, `409 Conflict` if period already PAID

---

## GET /api/contracts/{id}/periods

List all billing periods for a contract.

**Response** `200 OK` — `ApiResponse<List<ContractPeriodDto>>`

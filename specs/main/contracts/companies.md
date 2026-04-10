# API Contract: Companies

Base path: `/api/companies`
All responses: `ApiResponse<T>` envelope — `{ "success": bool, "data": T|null, "error": string|null }`

---

## POST /api/companies

Create a new company.

**Request body**
```json
{
  "name": "Acme Corp",
  "tier": "SMB",
  "industry": "IT Services",
  "region": "EMEA"
}
```

| Field | Type | Required | Values |
|-------|------|----------|--------|
| `name` | string | ✅ | non-blank |
| `tier` | enum | ✅ | `SMB`, `MID_MARKET`, `ENTERPRISE` |
| `industry` | string | ✗ | free text |
| `region` | string | ✗ | free text |

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Acme Corp",
    "industry": "IT Services",
    "region": "EMEA",
    "tier": "SMB",
    "status": "PROSPECT",
    "acquiredDate": null,
    "createdAt": "2026-04-10T10:00:00"
  },
  "error": null
}
```

**Error responses**
- `400 Bad Request` — validation failure (missing `name` or `tier`)

---

## GET /api/companies

List all companies, optionally filtered by status.

**Query params**

| Param | Type | Required | Notes |
|-------|------|----------|-------|
| `status` | enum | ✗ | `PROSPECT`, `ACTIVE`, `AT_RISK`, `CHURNED` |

**Response** `200 OK` — `ApiResponse<List<CompanyDto>>`

---

## GET /api/companies/{id}

Get a single company by UUID.

**Response** `200 OK` — `ApiResponse<CompanyDto>`
**Error**: `404 Not Found` if company does not exist.

---

## PUT /api/companies/{id}

Update company fields and/or status.

**Request body**
```json
{
  "name": "Acme Corp",
  "tier": "MID_MARKET",
  "industry": "IT Services",
  "region": "APAC",
  "status": "ACTIVE"
}
```

| Field | Required |
|-------|----------|
| `name` | ✅ |
| `tier` | ✅ |
| `status` | ✅ |
| `industry`, `region` | ✗ |

**Response** `200 OK` — `ApiResponse<CompanyDto>`

---

## POST /api/companies/{id}/churn

Mark a company as churned. Sets `status = CHURNED` and `churned_date = today`.

**Request body**: none
**Response** `200 OK` — `ApiResponse<Void>` (`data: null`)
**Error**: `404 Not Found`

---

## DELETE /api/companies/{id}

Delete a company and all cascading entities (contacts, interactions, etc.).

**Response** `204 No Content`
**Error**: `404 Not Found`

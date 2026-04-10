# API Contract: Invoices

Base path: `/api/invoices`
All responses: `ApiResponse<T>` envelope.

---

## POST /api/invoices

Create a new invoice (starts in DRAFT status).

**Request body**
```json
{
  "companyId": "550e8400-e29b-41d4-a716-446655440000",
  "invoiceNumber": "INV-2026-001",
  "issueDate": "2026-04-01",
  "dueDate": "2026-04-30",
  "notes": "Q1 managed services"
}
```

| Field | Type | Required |
|-------|------|----------|
| `companyId` | UUID | ✅ |
| `invoiceNumber` | string | ✅ (unique) |
| `issueDate` | date | ✅ |
| `dueDate` | date | ✅ |
| `notes` | string | ✗ |

**Response** `201 Created` — `ApiResponse<InvoiceDto>`

**Error**: `409 Conflict` if `invoiceNumber` already exists.

---

## POST /api/invoices/{id}/line-items

Add a line item to an invoice. Updates `total_amount` automatically.

**Request body**
```json
{
  "description": "Monthly monitoring fee",
  "quantity": 1,
  "unitPrice": 1200.00,
  "serviceCategory": "MANAGED_SERVICES"
}
```

| Field | Type | Required |
|-------|------|----------|
| `description` | string | ✅ |
| `quantity` | decimal | ✅ (> 0) |
| `unitPrice` | decimal | ✅ (> 0) |
| `serviceCategory` | string | ✗ |

**Response** `201 Created` — `ApiResponse<InvoiceLineItemDto>`

```json
{
  "success": true,
  "data": {
    "id": "...",
    "invoiceId": "...",
    "description": "Monthly monitoring fee",
    "quantity": 1,
    "unitPrice": 1200.00,
    "lineTotal": 1200.00,
    "serviceCategory": "MANAGED_SERVICES"
  }
}
```

---

## POST /api/invoices/{id}/pay

Mark an invoice as paid. Sets `status = PAID` and `paid_date`.

**Query params**

| Param | Type | Required | Notes |
|-------|------|----------|-------|
| `paidDate` | date (ISO) | ✗ | Defaults to today if omitted |

**Response** `200 OK` — `ApiResponse<InvoiceDto>` (updated record)
**Error**: `404 Not Found`, `409 Conflict` if already PAID or CANCELLED

---

## GET /api/invoices/company/{companyId}

List all invoices for a company.

**Response** `200 OK` — `ApiResponse<List<InvoiceDto>>`

---

## GET /api/invoices/{id}

Get a single invoice (including line items).

**Response** `200 OK` — `ApiResponse<InvoiceDto>`
**Error**: `404 Not Found`

---

## InvoiceDto shape

```json
{
  "id": "...",
  "companyId": "...",
  "invoiceNumber": "INV-2026-001",
  "issueDate": "2026-04-01",
  "dueDate": "2026-04-30",
  "totalAmount": 1200.00,
  "status": "PAID",
  "paidDate": "2026-04-15",
  "notes": "Q1 managed services",
  "lineItems": [...]
}
```

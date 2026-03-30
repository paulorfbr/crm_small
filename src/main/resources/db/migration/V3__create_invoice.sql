CREATE TABLE invoice (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID           NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    invoice_number VARCHAR(50)    NOT NULL UNIQUE,
    issued_date    DATE           NOT NULL,
    due_date       DATE           NOT NULL,
    paid_date      DATE,
    status         VARCHAR(20)    NOT NULL DEFAULT 'DRAFT'
                   CHECK (status IN ('DRAFT','SENT','PAID','OVERDUE','CANCELLED')),
    total_amount   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    notes          TEXT,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE invoice_line_item (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id       UUID          NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    description      VARCHAR(500)  NOT NULL,
    quantity         DECIMAL(10,2) NOT NULL,
    unit_price       DECIMAL(12,2) NOT NULL,
    line_total       DECIMAL(12,2) NOT NULL,
    service_category VARCHAR(100)
);

CREATE INDEX idx_invoice_company  ON invoice(company_id);
CREATE INDEX idx_invoice_paid     ON invoice(paid_date) WHERE status = 'PAID';
CREATE INDEX idx_line_item_invoice ON invoice_line_item(invoice_id);
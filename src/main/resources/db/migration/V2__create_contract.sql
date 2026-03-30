CREATE TABLE contract (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID           NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    service_type  VARCHAR(50)    NOT NULL
                  CHECK (service_type IN ('MANAGED_SERVICES','SUPPORT_RETAINER','SAAS_LICENSE','CONSULTING_RETAINER')),
    description   VARCHAR(500),
    monthly_value DECIMAL(12,2)  NOT NULL,
    start_date    DATE           NOT NULL,
    end_date      DATE,
    status        VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE','EXPIRED','CANCELLED','PENDING_RENEWAL')),
    auto_renews   BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_period (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id    UUID          NOT NULL REFERENCES contract(id) ON DELETE CASCADE,
    company_id     UUID          NOT NULL REFERENCES company(id),
    period_start   DATE          NOT NULL,
    period_end     DATE          NOT NULL,
    amount_billed  DECIMAL(12,2) NOT NULL,
    paid_at        TIMESTAMP,
    status         VARCHAR(10)   NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING','PAID','OVERDUE'))
);

CREATE INDEX idx_contract_company        ON contract(company_id);
CREATE INDEX idx_contract_period_company ON contract_period(company_id);
CREATE INDEX idx_contract_period_paid    ON contract_period(paid_at) WHERE status = 'PAID';
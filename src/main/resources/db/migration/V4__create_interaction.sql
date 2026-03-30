CREATE TABLE interaction (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID         NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    contact_id      UUID         REFERENCES contact(id) ON DELETE SET NULL,
    type            VARCHAR(30)  NOT NULL
                    CHECK (type IN ('CALL','EMAIL','MEETING','SUPPORT_TICKET','RENEWAL_DISCUSSION')),
    occurred_at     TIMESTAMP    NOT NULL,
    notes           TEXT,
    outcome         VARCHAR(255),
    created_by_user VARCHAR(100),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interaction_company    ON interaction(company_id);
CREATE INDEX idx_interaction_occurred   ON interaction(occurred_at DESC);
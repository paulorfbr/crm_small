CREATE TABLE company (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    industry      VARCHAR(100),
    region        VARCHAR(100),
    tier          VARCHAR(20)  NOT NULL DEFAULT 'SMB'
                  CHECK (tier IN ('SMB', 'MID_MARKET', 'ENTERPRISE')),
    status        VARCHAR(20)  NOT NULL DEFAULT 'PROSPECT'
                  CHECK (status IN ('PROSPECT', 'ACTIVE', 'AT_RISK', 'CHURNED')),
    acquired_date DATE,
    churned_date  DATE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE contact (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID         NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255) UNIQUE,
    phone       VARCHAR(50),
    role        VARCHAR(100),
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contact_company ON contact(company_id);
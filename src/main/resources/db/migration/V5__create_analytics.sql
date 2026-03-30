CREATE TABLE company_rfm_score (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id       UUID          NOT NULL UNIQUE REFERENCES company(id) ON DELETE CASCADE,
    recency_days     INTEGER       NOT NULL,
    recency_score    SMALLINT      NOT NULL CHECK (recency_score BETWEEN 1 AND 5),
    frequency_count  INTEGER       NOT NULL,
    frequency_score  SMALLINT      NOT NULL CHECK (frequency_score BETWEEN 1 AND 5),
    monetary_total   DECIMAL(14,2) NOT NULL,
    monetary_score   SMALLINT      NOT NULL CHECK (monetary_score BETWEEN 1 AND 5),
    rfm_score        SMALLINT      NOT NULL,
    segment          VARCHAR(30)   NOT NULL
                     CHECK (segment IN ('CHAMPION','LOYAL','POTENTIAL_LOYALIST','NEW',
                                        'AT_RISK','CANT_LOSE','HIBERNATING','LOST')),
    calculated_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE company_ltv_record (
    id                        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id                UUID          NOT NULL UNIQUE REFERENCES company(id) ON DELETE CASCADE,
    historical_revenue        DECIMAL(14,2) NOT NULL DEFAULT 0,
    projected_annual_value    DECIMAL(14,2) NOT NULL DEFAULT 0,
    projected_ltv             DECIMAL(14,2) NOT NULL DEFAULT 0,
    avg_monthly_revenue       DECIMAL(14,2) NOT NULL DEFAULT 0,
    estimated_churn_risk      VARCHAR(10)   NOT NULL DEFAULT 'LOW'
                              CHECK (estimated_churn_risk IN ('LOW','MEDIUM','HIGH')),
    contract_months_remaining INTEGER       NOT NULL DEFAULT 0,
    calculated_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);
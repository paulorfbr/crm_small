CREATE TABLE company_bcg_record (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id              UUID          NOT NULL UNIQUE REFERENCES company(id) ON DELETE CASCADE,
    revenue_share_pct       DECIMAL(6,3)  NOT NULL DEFAULT 0,   -- % of total portfolio revenue
    revenue_growth_rate_pct DECIMAL(8,3)  NOT NULL DEFAULT 0,   -- YoY growth %
    current_year_revenue    DECIMAL(14,2) NOT NULL DEFAULT 0,
    previous_year_revenue   DECIMAL(14,2) NOT NULL DEFAULT 0,
    quadrant                VARCHAR(20)   NOT NULL
                            CHECK (quadrant IN ('STAR','CASH_COW','QUESTION_MARK','DOG')),
    calculated_at           TIMESTAMP     NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN company_bcg_record.revenue_share_pct IS
    'Relative revenue share of this company vs total portfolio. Threshold at median.';
COMMENT ON COLUMN company_bcg_record.revenue_growth_rate_pct IS
    'YoY revenue growth rate in %. Threshold at portfolio median growth.';

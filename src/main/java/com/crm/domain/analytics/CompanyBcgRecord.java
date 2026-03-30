package com.crm.domain.analytics;

import com.crm.domain.company.Company;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_bcg_record")
public class CompanyBcgRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    /** Percentage of total portfolio revenue this company represents. */
    @Column(nullable = false)
    private BigDecimal revenueSharePct = BigDecimal.ZERO;

    /** Year-over-year revenue growth rate in %. Negative = shrinking account. */
    @Column(nullable = false)
    private BigDecimal revenueGrowthRatePct = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal currentYearRevenue = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal previousYearRevenue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BcgQuadrant quadrant;

    @Column(nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();

    protected CompanyBcgRecord() {}

    public CompanyBcgRecord(Company company) {
        this.company = company;
    }

    public void update(BigDecimal revenueSharePct, BigDecimal revenueGrowthRatePct,
                       BigDecimal currentYearRevenue, BigDecimal previousYearRevenue,
                       BcgQuadrant quadrant) {
        this.revenueSharePct = revenueSharePct;
        this.revenueGrowthRatePct = revenueGrowthRatePct;
        this.currentYearRevenue = currentYearRevenue;
        this.previousYearRevenue = previousYearRevenue;
        this.quadrant = quadrant;
        this.calculatedAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public BigDecimal getRevenueSharePct() { return revenueSharePct; }
    public BigDecimal getRevenueGrowthRatePct() { return revenueGrowthRatePct; }
    public BigDecimal getCurrentYearRevenue() { return currentYearRevenue; }
    public BigDecimal getPreviousYearRevenue() { return previousYearRevenue; }
    public BcgQuadrant getQuadrant() { return quadrant; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
}

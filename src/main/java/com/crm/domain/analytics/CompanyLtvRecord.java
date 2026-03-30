package com.crm.domain.analytics;

import com.crm.domain.company.Company;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_ltv_record")
public class CompanyLtvRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(nullable = false)
    private BigDecimal historicalRevenue = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal projectedAnnualValue = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal projectedLtv = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal avgMonthlyRevenue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChurnRisk estimatedChurnRisk = ChurnRisk.LOW;

    @Column(nullable = false)
    private int contractMonthsRemaining = 0;

    @Column(nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();

    protected CompanyLtvRecord() {}

    public CompanyLtvRecord(Company company) {
        this.company = company;
    }

    public void update(BigDecimal historicalRevenue, BigDecimal projectedAnnualValue,
                       BigDecimal projectedLtv, BigDecimal avgMonthlyRevenue,
                       ChurnRisk estimatedChurnRisk, int contractMonthsRemaining) {
        this.historicalRevenue = historicalRevenue;
        this.projectedAnnualValue = projectedAnnualValue;
        this.projectedLtv = projectedLtv;
        this.avgMonthlyRevenue = avgMonthlyRevenue;
        this.estimatedChurnRisk = estimatedChurnRisk;
        this.contractMonthsRemaining = contractMonthsRemaining;
        this.calculatedAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public BigDecimal getHistoricalRevenue() { return historicalRevenue; }
    public BigDecimal getProjectedAnnualValue() { return projectedAnnualValue; }
    public BigDecimal getProjectedLtv() { return projectedLtv; }
    public BigDecimal getAvgMonthlyRevenue() { return avgMonthlyRevenue; }
    public ChurnRisk getEstimatedChurnRisk() { return estimatedChurnRisk; }
    public int getContractMonthsRemaining() { return contractMonthsRemaining; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
}

package com.crm.domain.contract;

import com.crm.domain.company.Company;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract_period")
public class ContractPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private BigDecimal amountBilled;

    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractPeriodStatus status = ContractPeriodStatus.PENDING;

    protected ContractPeriod() {}

    public ContractPeriod(Contract contract, Company company,
                          LocalDate periodStart, LocalDate periodEnd,
                          BigDecimal amountBilled) {
        this.contract = contract;
        this.company = company;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.amountBilled = amountBilled;
    }

    public void markPaid() {
        if (this.status == ContractPeriodStatus.PAID) {
            throw new IllegalStateException("Contract period is already paid: " + id);
        }
        this.status = ContractPeriodStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public Contract getContract() { return contract; }
    public Company getCompany() { return company; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getAmountBilled() { return amountBilled; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public ContractPeriodStatus getStatus() { return status; }

    public void setStatus(ContractPeriodStatus status) { this.status = status; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}

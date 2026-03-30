package com.crm.domain.contract;

import com.crm.domain.company.Company;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    private String description;

    @Column(nullable = false)
    private BigDecimal monthlyValue;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status = ContractStatus.ACTIVE;

    @Column(nullable = false)
    private boolean autoRenews = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Contract() {}

    public Contract(Company company, ServiceType serviceType, BigDecimal monthlyValue, LocalDate startDate) {
        this.company = company;
        this.serviceType = serviceType;
        this.monthlyValue = monthlyValue;
        this.startDate = startDate;
    }

    // Getters
    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public ServiceType getServiceType() { return serviceType; }
    public String getDescription() { return description; }
    public BigDecimal getMonthlyValue() { return monthlyValue; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public ContractStatus getStatus() { return status; }
    public boolean isAutoRenews() { return autoRenews; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setDescription(String description) { this.description = description; }
    public void setMonthlyValue(BigDecimal monthlyValue) { this.monthlyValue = monthlyValue; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setStatus(ContractStatus status) { this.status = status; }
    public void setAutoRenews(boolean autoRenews) { this.autoRenews = autoRenews; }
}

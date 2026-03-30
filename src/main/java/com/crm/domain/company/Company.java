package com.crm.domain.company;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String industry;
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyTier tier = CompanyTier.SMB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status = CompanyStatus.PROSPECT;

    private LocalDate acquiredDate;
    private LocalDate churnedDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    protected Company() {}

    public Company(String name, CompanyTier tier) {
        this.name = name;
        this.tier = tier;
    }

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getIndustry() { return industry; }
    public String getRegion() { return region; }
    public CompanyTier getTier() { return tier; }
    public CompanyStatus getStatus() { return status; }
    public LocalDate getAcquiredDate() { return acquiredDate; }
    public LocalDate getChurnedDate() { return churnedDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setIndustry(String industry) { this.industry = industry; }
    public void setRegion(String region) { this.region = region; }
    public void setTier(CompanyTier tier) { this.tier = tier; }
    public void setStatus(CompanyStatus status) { this.status = status; }
    public void setAcquiredDate(LocalDate acquiredDate) { this.acquiredDate = acquiredDate; }
    public void setChurnedDate(LocalDate churnedDate) { this.churnedDate = churnedDate; }
}

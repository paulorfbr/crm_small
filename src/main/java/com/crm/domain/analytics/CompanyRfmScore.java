package com.crm.domain.analytics;

import com.crm.domain.company.Company;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_rfm_score")
public class CompanyRfmScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    private int recencyDays;
    private short recencyScore;
    private int frequencyCount;
    private short frequencyScore;
    private BigDecimal monetaryTotal;
    private short monetaryScore;
    private short rfmScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RfmSegment segment;

    @Column(nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();

    protected CompanyRfmScore() {}

    public CompanyRfmScore(Company company) {
        this.company = company;
    }

    public void update(int recencyDays, short recencyScore,
                       int frequencyCount, short frequencyScore,
                       BigDecimal monetaryTotal, short monetaryScore,
                       RfmSegment segment) {
        this.recencyDays = recencyDays;
        this.recencyScore = recencyScore;
        this.frequencyCount = frequencyCount;
        this.frequencyScore = frequencyScore;
        this.monetaryTotal = monetaryTotal;
        this.monetaryScore = monetaryScore;
        this.rfmScore = (short) (recencyScore * 100 + frequencyScore * 10 + monetaryScore);
        this.segment = segment;
        this.calculatedAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public int getRecencyDays() { return recencyDays; }
    public short getRecencyScore() { return recencyScore; }
    public int getFrequencyCount() { return frequencyCount; }
    public short getFrequencyScore() { return frequencyScore; }
    public BigDecimal getMonetaryTotal() { return monetaryTotal; }
    public short getMonetaryScore() { return monetaryScore; }
    public short getRfmScore() { return rfmScore; }
    public RfmSegment getSegment() { return segment; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
}

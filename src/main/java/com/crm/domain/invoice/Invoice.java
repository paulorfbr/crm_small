package com.crm.domain.invoice;

import com.crm.domain.company.Company;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private LocalDate issuedDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    protected Invoice() {}

    public Invoice(Company company, String invoiceNumber, LocalDate issuedDate, LocalDate dueDate) {
        this.company = company;
        this.invoiceNumber = invoiceNumber;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
    }

    public void addLineItem(InvoiceLineItem item) {
        item.setInvoice(this);
        lineItems.add(item);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = lineItems.stream()
                .map(InvoiceLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void markPaid(LocalDate paidDate) {
        this.paidDate = paidDate;
        this.status = InvoiceStatus.PAID;
    }

    // Getters
    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public LocalDate getIssuedDate() { return issuedDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getPaidDate() { return paidDate; }
    public InvoiceStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<InvoiceLineItem> getLineItems() { return lineItems; }

    // Setters
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}

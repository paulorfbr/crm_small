package com.crm.domain.interaction;

import com.crm.domain.company.Company;
import com.crm.domain.contact.Contact;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interaction")
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InteractionType type;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    private String notes;
    private String outcome;
    private String createdByUser;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Interaction() {}

    public Interaction(Company company, InteractionType type, LocalDateTime occurredAt) {
        this.company = company;
        this.type = type;
        this.occurredAt = occurredAt;
    }

    // Getters
    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public Contact getContact() { return contact; }
    public InteractionType getType() { return type; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getNotes() { return notes; }
    public String getOutcome() { return outcome; }
    public String getCreatedByUser() { return createdByUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setContact(Contact contact) { this.contact = contact; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public void setCreatedByUser(String createdByUser) { this.createdByUser = createdByUser; }
}

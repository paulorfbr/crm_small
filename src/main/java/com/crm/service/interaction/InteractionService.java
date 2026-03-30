package com.crm.service.interaction;

import com.crm.domain.company.Company;
import com.crm.domain.contact.Contact;
import com.crm.domain.interaction.Interaction;
import com.crm.domain.interaction.InteractionType;
import com.crm.repository.CompanyRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.InteractionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;

    public InteractionService(InteractionRepository interactionRepository,
                               CompanyRepository companyRepository,
                               ContactRepository contactRepository) {
        this.interactionRepository = interactionRepository;
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
    }

    public Interaction log(UUID companyId, UUID contactId, InteractionType type,
                           LocalDateTime occurredAt, String notes, String outcome,
                           String createdByUser) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
        Interaction interaction = new Interaction(company, type, occurredAt);
        if (contactId != null) {
            Contact contact = contactRepository.findById(contactId)
                    .orElseThrow(() -> new EntityNotFoundException("Contact not found: " + contactId));
            interaction.setContact(contact);
        }
        interaction.setNotes(notes);
        interaction.setOutcome(outcome);
        interaction.setCreatedByUser(createdByUser);
        return interactionRepository.save(interaction);
    }

    @Transactional(readOnly = true)
    public List<Interaction> getRecentByCompany(UUID companyId, int limit) {
        return interactionRepository.findByCompanyIdOrderByOccurredAtDesc(
                companyId, PageRequest.of(0, limit));
    }
}

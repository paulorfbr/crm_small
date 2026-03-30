package com.crm.repository;

import com.crm.domain.contact.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
    List<Contact> findByCompanyId(UUID companyId);
}

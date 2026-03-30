package com.crm.repository;

import com.crm.domain.interaction.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface InteractionRepository extends JpaRepository<Interaction, UUID> {
    List<Interaction> findByCompanyIdOrderByOccurredAtDesc(UUID companyId, Pageable pageable);
}

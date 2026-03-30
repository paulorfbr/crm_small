package com.crm.repository;

import com.crm.domain.contract.Contract;
import com.crm.domain.contract.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    List<Contract> findByCompanyIdAndStatus(UUID companyId, ContractStatus status);

    List<Contract> findByStatus(ContractStatus status);

    /** Contracts expiring within the next {@code days} days (renewal alert). */
    @Query("""
           SELECT c FROM Contract c
           WHERE c.status = 'ACTIVE'
             AND c.endDate IS NOT NULL
             AND c.endDate BETWEEN :today AND :alertDate
           """)
    List<Contract> findExpiringBetween(@Param("today") LocalDate today,
                                       @Param("alertDate") LocalDate alertDate);
}

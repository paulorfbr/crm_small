package com.crm.repository;

import com.crm.domain.contract.ContractPeriod;
import com.crm.domain.contract.ContractPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ContractPeriodRepository extends JpaRepository<ContractPeriod, UUID> {

    List<ContractPeriod> findByContractId(UUID contractId);

    List<ContractPeriod> findByCompanyIdAndStatus(UUID companyId, ContractPeriodStatus status);

    @Query("SELECT COALESCE(SUM(cp.amountBilled), 0) FROM ContractPeriod cp " +
           "WHERE cp.company.id = :companyId AND cp.status = 'PAID'")
    BigDecimal sumPaidByCompany(@Param("companyId") UUID companyId);
}

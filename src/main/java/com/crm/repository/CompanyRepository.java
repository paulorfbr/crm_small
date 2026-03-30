package com.crm.repository;

import com.crm.domain.company.Company;
import com.crm.domain.company.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    List<Company> findByStatus(CompanyStatus status);
    boolean existsByName(String name);
}

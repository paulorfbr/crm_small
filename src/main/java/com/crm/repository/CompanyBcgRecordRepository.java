package com.crm.repository;

import com.crm.domain.analytics.BcgQuadrant;
import com.crm.domain.analytics.CompanyBcgRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyBcgRecordRepository extends JpaRepository<CompanyBcgRecord, UUID> {

    Optional<CompanyBcgRecord> findByCompanyId(UUID companyId);

    List<CompanyBcgRecord> findByQuadrant(BcgQuadrant quadrant);

    @org.springframework.data.jpa.repository.Query(
            "SELECT b.quadrant, COUNT(b) FROM CompanyBcgRecord b GROUP BY b.quadrant")
    List<Object[]> countByQuadrant();
}

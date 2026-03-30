package com.crm.repository;

import com.crm.domain.analytics.CompanyLtvRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyLtvRecordRepository extends JpaRepository<CompanyLtvRecord, UUID> {

    Optional<CompanyLtvRecord> findByCompanyId(UUID companyId);

    @Query("SELECT l FROM CompanyLtvRecord l ORDER BY l.projectedLtv DESC")
    List<CompanyLtvRecord> findTopByProjectedLtv(Pageable pageable);
}

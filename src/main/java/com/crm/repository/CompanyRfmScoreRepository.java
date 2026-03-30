package com.crm.repository;

import com.crm.domain.analytics.CompanyRfmScore;
import com.crm.domain.analytics.RfmSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRfmScoreRepository extends JpaRepository<CompanyRfmScore, UUID> {

    Optional<CompanyRfmScore> findByCompanyId(UUID companyId);

    List<CompanyRfmScore> findBySegment(RfmSegment segment);

    @Query("SELECT r.segment, COUNT(r) FROM CompanyRfmScore r GROUP BY r.segment")
    List<Object[]> countBySegment();

    /** Companies with high monetary score but low recency — churn candidates. */
    @Query("""
           SELECT r FROM CompanyRfmScore r
           WHERE r.recencyScore <= 2
             AND r.monetaryScore >= 3
           ORDER BY r.monetaryTotal DESC
           """)
    List<CompanyRfmScore> findAtRiskHighValue();
}

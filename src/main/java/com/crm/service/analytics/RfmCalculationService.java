package com.crm.service.analytics;

import com.crm.domain.analytics.CompanyRfmScore;
import com.crm.domain.analytics.RfmSegment;
import com.crm.domain.company.Company;
import com.crm.domain.company.CompanyStatus;
import com.crm.repository.CompanyRepository;
import com.crm.repository.CompanyRfmScoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class RfmCalculationService {

    private static final Logger log = LoggerFactory.getLogger(RfmCalculationService.class);

    @PersistenceContext
    private EntityManager em;

    private final CompanyRepository companyRepository;
    private final CompanyRfmScoreRepository rfmScoreRepository;

    public RfmCalculationService(CompanyRepository companyRepository,
                                 CompanyRfmScoreRepository rfmScoreRepository) {
        this.companyRepository = companyRepository;
        this.rfmScoreRepository = rfmScoreRepository;
    }

    /**
     * Recalculates RFM scores for all companies that have at least one paid event.
     * Uses PostgreSQL NTILE(5) window function for percentile-based scoring.
     */
    @Transactional
    public void recalculateAll() {
        log.info("Starting RFM recalculation");

        List<Object[]> rows = em.createNativeQuery("""
                WITH payment_events AS (
                    SELECT cp.company_id,
                           cp.paid_at        AS event_date,
                           cp.amount_billed  AS amount
                    FROM contract_period cp
                    WHERE cp.status = 'PAID'
                    UNION ALL
                    SELECT i.company_id,
                           i.paid_date::TIMESTAMP AS event_date,
                           i.total_amount         AS amount
                    FROM invoice i
                    WHERE i.status = 'PAID'
                ),
                rfm_raw AS (
                    SELECT company_id,
                           EXTRACT(DAY FROM NOW() - MAX(event_date))::INT AS recency_days,
                           COUNT(*)::INT                                   AS frequency_count,
                           SUM(amount)                                     AS monetary_total
                    FROM payment_events
                    GROUP BY company_id
                ),
                scored AS (
                    SELECT company_id,
                           recency_days,
                           frequency_count,
                           monetary_total,
                           (6 - NTILE(5) OVER (ORDER BY recency_days ASC))::SMALLINT  AS recency_score,
                           NTILE(5)       OVER (ORDER BY frequency_count ASC)::SMALLINT AS frequency_score,
                           NTILE(5)       OVER (ORDER BY monetary_total  ASC)::SMALLINT AS monetary_score
                    FROM rfm_raw
                )
                SELECT company_id::TEXT,
                       recency_days,
                       recency_score,
                       frequency_count,
                       frequency_score,
                       monetary_total,
                       monetary_score
                FROM scored
                """).getResultList();

        if (rows.isEmpty()) {
            log.info("No paid payment events found — skipping RFM scoring");
            return;
        }

        Map<UUID, Company> companyCache = new HashMap<>();
        for (Company c : companyRepository.findAll()) {
            companyCache.put(c.getId(), c);
        }

        for (Object[] row : rows) {
            UUID companyId = UUID.fromString((String) row[0]);
            int recencyDays    = ((Number) row[1]).intValue();
            short recencyScore = ((Number) row[2]).shortValue();
            int frequencyCount = ((Number) row[3]).intValue();
            short freqScore    = ((Number) row[4]).shortValue();
            BigDecimal monetary = (BigDecimal) row[5];
            short monScore     = ((Number) row[6]).shortValue();

            RfmSegment segment = assignSegment(recencyScore, freqScore, monScore);

            CompanyRfmScore score = rfmScoreRepository.findByCompanyId(companyId)
                    .orElseGet(() -> new CompanyRfmScore(companyCache.get(companyId)));

            score.update(recencyDays, recencyScore, frequencyCount, freqScore,
                         monetary, monScore, segment);
            rfmScoreRepository.save(score);

            // Promote company from PROSPECT to ACTIVE if they have paid events
            Company company = companyCache.get(companyId);
            if (company != null && company.getStatus() == CompanyStatus.PROSPECT) {
                company.setStatus(CompanyStatus.ACTIVE);
                companyRepository.save(company);
            }

            // Flag company as AT_RISK when segment degrades
            if (company != null
                    && company.getStatus() == CompanyStatus.ACTIVE
                    && (segment == RfmSegment.AT_RISK || segment == RfmSegment.CANT_LOSE)) {
                company.setStatus(CompanyStatus.AT_RISK);
                companyRepository.save(company);
            }
        }

        log.info("RFM recalculation complete — {} companies scored", rows.size());
    }

    /**
     * Assigns an RFM segment based on the three individual scores (1–5).
     * Rules are evaluated in order; first match wins.
     */
    static RfmSegment assignSegment(short r, short f, short m) {
        if (r >= 4 && f >= 4 && m >= 4)            return RfmSegment.CHAMPION;
        if (r <= 2 && f >= 4 && m >= 4)            return RfmSegment.CANT_LOSE;
        if (r <= 2 && f >= 3 && m >= 3)            return RfmSegment.AT_RISK;
        if (r >= 3 && f >= 4)                       return RfmSegment.LOYAL;
        if (r == 5 && f == 1)                       return RfmSegment.NEW;
        if (r >= 4 && f <= 2)                       return RfmSegment.POTENTIAL_LOYALIST;
        if (r <= 2 && f <= 2)                       return RfmSegment.HIBERNATING;
        if (r == 1 && f == 1)                       return RfmSegment.LOST;
        return RfmSegment.LOYAL; // default: decent engagement
    }
}

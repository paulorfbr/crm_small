package com.crm.service.analytics;

import com.crm.domain.analytics.BcgQuadrant;
import com.crm.domain.analytics.CompanyBcgRecord;
import com.crm.domain.company.Company;
import com.crm.repository.CompanyBcgRecordRepository;
import com.crm.repository.CompanyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Calculates BCG Growth-Share Matrix positions for all companies.
 *
 * <p>Axes (adapted for B2B customer portfolio):
 * <ul>
 *   <li><b>Revenue Share</b> — this company's revenue as % of total portfolio revenue
 *       (X-axis proxy for "relative market share")</li>
 *   <li><b>Revenue Growth</b> — YoY revenue growth rate in %
 *       (Y-axis = "market growth rate")</li>
 * </ul>
 *
 * <p>Thresholds are <b>relative</b> (portfolio medians), not fixed percentages.
 * This ensures the matrix is always populated across quadrants regardless of
 * portfolio size or revenue scale.
 */
@Service
public class BcgCalculationService {

    private static final Logger log = LoggerFactory.getLogger(BcgCalculationService.class);

    @PersistenceContext
    private EntityManager em;

    private final CompanyRepository companyRepository;
    private final CompanyBcgRecordRepository bcgRepository;

    public BcgCalculationService(CompanyRepository companyRepository,
                                  CompanyBcgRecordRepository bcgRepository) {
        this.companyRepository = companyRepository;
        this.bcgRepository = bcgRepository;
    }

    /**
     * Recalculates BCG quadrants for all companies with payment history.
     * Must be called after RFM and LTV recalculation (uses payment event data).
     */
    @Transactional
    public void recalculateAll() {
        log.info("Starting BCG recalculation");

        // Step 1: compute current-year and previous-year revenue per company
        int currentYear  = java.time.Year.now().getValue();
        int previousYear = currentYear - 1;

        List<Object[]> currentYearRows  = revenueByYear(currentYear);
        List<Object[]> previousYearRows = revenueByYear(previousYear);

        if (currentYearRows.isEmpty()) {
            log.info("No revenue data for current year — skipping BCG calculation");
            return;
        }

        Map<UUID, BigDecimal> currentMap  = toMap(currentYearRows);
        Map<UUID, BigDecimal> previousMap = toMap(previousYearRows);

        // Step 2: compute total portfolio revenue (current year) for share calculation
        BigDecimal totalRevenue = currentMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Total portfolio revenue is zero — skipping BCG calculation");
            return;
        }

        // Step 3: compute revenue share and growth rate for each company
        record CompanyMetrics(UUID companyId, BigDecimal shareP, BigDecimal growthP,
                              BigDecimal currentRev, BigDecimal prevRev) {}

        List<CompanyMetrics> metrics = new ArrayList<>();
        for (Map.Entry<UUID, BigDecimal> entry : currentMap.entrySet()) {
            UUID companyId  = entry.getKey();
            BigDecimal curr = entry.getValue();
            BigDecimal prev = previousMap.getOrDefault(companyId, BigDecimal.ZERO);

            BigDecimal shareP = curr
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 3, RoundingMode.HALF_UP);

            BigDecimal growthP;
            if (prev.compareTo(BigDecimal.ZERO) == 0) {
                // New customer this year — treat as maximum growth
                growthP = BigDecimal.valueOf(100);
            } else {
                growthP = curr.subtract(prev)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(prev, 3, RoundingMode.HALF_UP);
            }

            metrics.add(new CompanyMetrics(companyId, shareP, growthP, curr, prev));
        }

        // Step 4: compute portfolio medians as thresholds (relative classification)
        double shareMedian  = median(metrics.stream().map(m -> m.shareP().doubleValue()).toList());
        double growthMedian = median(metrics.stream().map(m -> m.growthP().doubleValue()).toList());

        log.info("BCG thresholds — share median: {}%, growth median: {}%",
                String.format("%.2f", shareMedian), String.format("%.2f", growthMedian));

        // Step 5: assign quadrant and persist
        Map<UUID, Company> companyCache = new HashMap<>();
        companyRepository.findAll().forEach(c -> companyCache.put(c.getId(), c));

        for (CompanyMetrics m : metrics) {
            BcgQuadrant quadrant = assignQuadrant(m.shareP().doubleValue(),
                    m.growthP().doubleValue(), shareMedian, growthMedian);

            CompanyBcgRecord record = bcgRepository.findByCompanyId(m.companyId())
                    .orElseGet(() -> new CompanyBcgRecord(companyCache.get(m.companyId())));

            record.update(m.shareP(), m.growthP(), m.currentRev(), m.prevRev(), quadrant);
            bcgRepository.save(record);
        }

        log.info("BCG recalculation complete — {} companies classified", metrics.size());
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> revenueByYear(int year) {
        return em.createNativeQuery("""
                WITH payment_events AS (
                    SELECT cp.company_id, cp.amount_billed AS amount,
                           EXTRACT(YEAR FROM cp.paid_at)::INT AS yr
                    FROM contract_period cp WHERE cp.status = 'PAID'
                    UNION ALL
                    SELECT i.company_id, i.total_amount AS amount,
                           EXTRACT(YEAR FROM i.paid_date)::INT AS yr
                    FROM invoice i WHERE i.status = 'PAID'
                )
                SELECT company_id::TEXT, SUM(amount) AS total
                FROM payment_events
                WHERE yr = :year
                GROUP BY company_id
                """)
                .setParameter("year", year)
                .getResultList();
    }

    private static Map<UUID, BigDecimal> toMap(List<Object[]> rows) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(UUID.fromString((String) row[0]), (BigDecimal) row[1]);
        }
        return map;
    }

    /**
     * Assigns a BCG quadrant using portfolio-relative thresholds.
     *
     * <pre>
     *              HIGH GROWTH
     *      QUESTION_MARK  |  STAR
     *      ———————————————+————————————  (growthMedian)
     *           DOG        |  CASH_COW
     *              LOW GROWTH
     *       LOW SHARE     HIGH SHARE
     *                 (shareMedian)
     * </pre>
     */
    static BcgQuadrant assignQuadrant(double shareP, double growthP,
                                       double shareMedian, double growthMedian) {
        boolean highShare  = shareP  >= shareMedian;
        boolean highGrowth = growthP >= growthMedian;

        if (highShare && highGrowth)   return BcgQuadrant.STAR;
        if (highShare)                 return BcgQuadrant.CASH_COW;
        if (highGrowth)                return BcgQuadrant.QUESTION_MARK;
        return BcgQuadrant.DOG;
    }

    static double median(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = values.stream().sorted().toList();
        int n = sorted.size();
        return n % 2 == 0
                ? (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0
                : sorted.get(n / 2);
    }
}

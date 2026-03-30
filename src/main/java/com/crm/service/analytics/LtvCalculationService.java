package com.crm.service.analytics;

import com.crm.domain.analytics.ChurnRisk;
import com.crm.domain.analytics.CompanyLtvRecord;
import com.crm.domain.analytics.CompanyRfmScore;
import com.crm.domain.analytics.RfmSegment;
import com.crm.domain.contract.Contract;
import com.crm.domain.contract.ContractStatus;
import com.crm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class LtvCalculationService {

    private static final Logger log = LoggerFactory.getLogger(LtvCalculationService.class);

    @Value("${crm.ltv.projection-months:24}")
    private int projectionMonths;

    @Value("${crm.ltv.project-revenue-discount-factor:0.7}")
    private double projectRevenueDiscountFactor;

    @Value("${crm.ltv.churn-risk-discount.medium:0.15}")
    private double mediumChurnDiscount;

    @Value("${crm.ltv.churn-risk-discount.high:0.35}")
    private double highChurnDiscount;

    private final CompanyRepository companyRepository;
    private final CompanyRfmScoreRepository rfmScoreRepository;
    private final CompanyLtvRecordRepository ltvRecordRepository;
    private final ContractRepository contractRepository;
    private final ContractPeriodRepository contractPeriodRepository;
    private final InvoiceRepository invoiceRepository;

    public LtvCalculationService(CompanyRepository companyRepository,
                                 CompanyRfmScoreRepository rfmScoreRepository,
                                 CompanyLtvRecordRepository ltvRecordRepository,
                                 ContractRepository contractRepository,
                                 ContractPeriodRepository contractPeriodRepository,
                                 InvoiceRepository invoiceRepository) {
        this.companyRepository = companyRepository;
        this.rfmScoreRepository = rfmScoreRepository;
        this.ltvRecordRepository = ltvRecordRepository;
        this.contractRepository = contractRepository;
        this.contractPeriodRepository = contractPeriodRepository;
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Recalculates LTV for all companies that have an RFM score.
     * Must be called AFTER RfmCalculationService.recalculateAll().
     */
    @Transactional
    public void recalculateAll() {
        log.info("Starting LTV recalculation");

        List<CompanyRfmScore> scores = rfmScoreRepository.findAll();
        for (CompanyRfmScore rfm : scores) {
            recalculateForCompany(rfm);
        }

        log.info("LTV recalculation complete — {} companies processed", scores.size());
    }

    private void recalculateForCompany(CompanyRfmScore rfm) {
        UUID companyId = rfm.getCompany().getId();

        // --- Historical revenue (already realized) ---
        BigDecimal contractRevenue = contractPeriodRepository.sumPaidByCompany(companyId);
        BigDecimal invoiceRevenue  = invoiceRepository.sumPaidByCompany(companyId);
        BigDecimal historicalRevenue = contractRevenue.add(invoiceRevenue);

        // --- Contracted future revenue (high confidence) ---
        List<Contract> activeContracts = contractRepository
                .findByCompanyIdAndStatus(companyId, ContractStatus.ACTIVE);

        BigDecimal contractedFuture = BigDecimal.ZERO;
        int totalContractMonthsRemaining = 0;
        BigDecimal projectedAnnualValue = BigDecimal.ZERO;

        for (Contract c : activeContracts) {
            long months = monthsRemaining(c);
            contractedFuture = contractedFuture.add(
                    c.getMonthlyValue().multiply(BigDecimal.valueOf(months)));
            totalContractMonthsRemaining += months;
            projectedAnnualValue = projectedAnnualValue.add(c.getMonthlyValue().multiply(BigDecimal.valueOf(12)));
        }

        // --- Projected project revenue (discounted, lower confidence) ---
        BigDecimal avgMonthlyProject = computeAvgMonthlyProjectRevenue(companyId, invoiceRevenue);
        BigDecimal projectedProject = avgMonthlyProject
                .multiply(BigDecimal.valueOf(projectionMonths))
                .multiply(BigDecimal.valueOf(projectRevenueDiscountFactor))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal rawLtv = historicalRevenue.add(contractedFuture).add(projectedProject);

        // --- Average monthly revenue (for dashboard display) ---
        BigDecimal avgMonthlyRevenue = computeAvgMonthlyTotal(companyId,
                contractRevenue, invoiceRevenue);

        // --- Churn adjustment ---
        ChurnRisk churnRisk = deriveChurnRisk(rfm.getSegment());
        BigDecimal churnDiscount = BigDecimal.valueOf(churnDiscountRate(churnRisk));
        BigDecimal projectedLtv = rawLtv
                .multiply(BigDecimal.ONE.subtract(churnDiscount))
                .setScale(2, RoundingMode.HALF_UP);

        // --- Persist ---
        CompanyLtvRecord record = ltvRecordRepository.findByCompanyId(companyId)
                .orElseGet(() -> new CompanyLtvRecord(rfm.getCompany()));

        record.update(historicalRevenue, projectedAnnualValue, projectedLtv,
                      avgMonthlyRevenue, churnRisk, totalContractMonthsRemaining);
        ltvRecordRepository.save(record);
    }

    private long monthsRemaining(Contract contract) {
        LocalDate endDate = contract.getEndDate();
        if (endDate == null) {
            // Open-ended contract: assume 12 months of future value
            return 12L;
        }
        long months = ChronoUnit.MONTHS.between(LocalDate.now(), endDate);
        return Math.max(months, 0L);
    }

    private BigDecimal computeAvgMonthlyProjectRevenue(UUID companyId,
                                                        BigDecimal totalInvoiceRevenue) {
        LocalDate firstPaid = invoiceRepository.findFirstPaidDateByCompany(companyId);
        if (firstPaid == null || totalInvoiceRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        long monthsSinceFirst = ChronoUnit.MONTHS.between(firstPaid, LocalDate.now());
        if (monthsSinceFirst < 1) monthsSinceFirst = 1;
        return totalInvoiceRevenue
                .divide(BigDecimal.valueOf(monthsSinceFirst), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeAvgMonthlyTotal(UUID companyId,
                                               BigDecimal contractRevenue,
                                               BigDecimal invoiceRevenue) {
        BigDecimal total = contractRevenue.add(invoiceRevenue);
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        LocalDate firstPaid = invoiceRepository.findFirstPaidDateByCompany(companyId);
        if (firstPaid == null) firstPaid = LocalDate.now().minusMonths(1);
        long months = ChronoUnit.MONTHS.between(firstPaid, LocalDate.now());
        if (months < 1) months = 1;
        return total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    static ChurnRisk deriveChurnRisk(RfmSegment segment) {
        return switch (segment) {
            case CHAMPION, LOYAL, POTENTIAL_LOYALIST, NEW -> ChurnRisk.LOW;
            case AT_RISK, HIBERNATING                      -> ChurnRisk.MEDIUM;
            case CANT_LOSE, LOST                           -> ChurnRisk.HIGH;
        };
    }

    private double churnDiscountRate(ChurnRisk risk) {
        return switch (risk) {
            case LOW    -> 0.0;
            case MEDIUM -> mediumChurnDiscount;
            case HIGH   -> highChurnDiscount;
        };
    }
}

package com.crm.service.analytics;

import com.crm.domain.contract.Contract;
import com.crm.domain.invoice.Invoice;
import com.crm.repository.ContractRepository;
import com.crm.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class AnalyticsScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsScheduler.class);

    private final RfmCalculationService rfmService;
    private final LtvCalculationService ltvService;
    private final BcgCalculationService bcgService;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;

    @Value("${crm.analytics.cron:0 0 2 * * *}")
    private String cron;

    public AnalyticsScheduler(RfmCalculationService rfmService,
                               LtvCalculationService ltvService,
                               BcgCalculationService bcgService,
                               ContractRepository contractRepository,
                               InvoiceRepository invoiceRepository) {
        this.rfmService = rfmService;
        this.ltvService = ltvService;
        this.bcgService = bcgService;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
    }

    /** Nightly analytics recalculation — cron configured in application.properties. */
    @Scheduled(cron = "${crm.analytics.cron:0 0 2 * * *}")
    public void runNightly() {
        log.info("=== Nightly analytics run started ===");
        try {
            rfmService.recalculateAll();   // Step 1: RFM (must come first)
            ltvService.recalculateAll();   // Step 2: LTV (reads RFM segments)
            bcgService.recalculateAll();   // Step 3: BCG (reads payment history)
            checkRenewalAlerts();
            checkOverdueInvoices();
        } catch (Exception e) {
            log.error("Nightly analytics run failed", e);
        }
        log.info("=== Nightly analytics run complete ===");
    }

    private void checkRenewalAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate in60Days = today.plusDays(60);
        List<Contract> expiring = contractRepository.findExpiringBetween(today, in60Days);
        if (!expiring.isEmpty()) {
            log.warn("RENEWAL ALERT: {} contract(s) expire within 60 days", expiring.size());
            expiring.forEach(c -> log.warn("  Contract {} for company {} expires {}",
                    c.getId(), c.getCompany().getName(), c.getEndDate()));
        }
    }

    private void checkOverdueInvoices() {
        LocalDate cutoff = LocalDate.now().minusDays(30);
        List<Invoice> overdue = invoiceRepository.findOverdueBefore(cutoff);
        if (!overdue.isEmpty()) {
            log.warn("OVERDUE ALERT: {} invoice(s) overdue > 30 days", overdue.size());
            overdue.forEach(i -> log.warn("  Invoice {} for company {} due {}",
                    i.getInvoiceNumber(), i.getCompany().getName(), i.getDueDate()));
        }
    }
}

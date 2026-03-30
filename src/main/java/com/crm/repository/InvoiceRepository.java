package com.crm.repository;

import com.crm.domain.invoice.Invoice;
import com.crm.domain.invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByCompanyId(UUID companyId);

    List<Invoice> findByCompanyIdAndStatus(UUID companyId, InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.company.id = :companyId AND i.status = 'PAID'")
    BigDecimal sumPaidByCompany(@Param("companyId") UUID companyId);

    @Query("SELECT MIN(i.paidDate) FROM Invoice i " +
           "WHERE i.company.id = :companyId AND i.status = 'PAID'")
    LocalDate findFirstPaidDateByCompany(@Param("companyId") UUID companyId);

    /** Invoices overdue for more than {@code days} days. */
    @Query("""
           SELECT i FROM Invoice i
           WHERE i.status = 'SENT'
             AND i.dueDate < :cutoffDate
           """)
    List<Invoice> findOverdueBefore(@Param("cutoffDate") LocalDate cutoffDate);
}

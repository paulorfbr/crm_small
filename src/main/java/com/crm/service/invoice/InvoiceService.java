package com.crm.service.invoice;

import com.crm.domain.company.Company;
import com.crm.domain.invoice.Invoice;
import com.crm.domain.invoice.InvoiceLineItem;
import com.crm.domain.invoice.InvoiceStatus;
import com.crm.repository.CompanyRepository;
import com.crm.repository.InvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          CompanyRepository companyRepository) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
    }

    public Invoice create(UUID companyId, String invoiceNumber,
                          LocalDate issuedDate, LocalDate dueDate, String notes) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
        Invoice invoice = new Invoice(company, invoiceNumber, issuedDate, dueDate);
        invoice.setNotes(notes);
        return invoiceRepository.save(invoice);
    }

    public Invoice addLineItem(UUID invoiceId, String description,
                               BigDecimal quantity, BigDecimal unitPrice,
                               String serviceCategory) {
        Invoice invoice = getById(invoiceId);
        invoice.addLineItem(new InvoiceLineItem(description, quantity, unitPrice, serviceCategory));
        return invoiceRepository.save(invoice);
    }

    public Invoice markPaid(UUID invoiceId, LocalDate paidDate) {
        Invoice invoice = getById(invoiceId);
        invoice.markPaid(paidDate);
        return invoiceRepository.save(invoice);
    }

    public Invoice updateStatus(UUID invoiceId, InvoiceStatus status) {
        Invoice invoice = getById(invoiceId);
        invoice.setStatus(status);
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice getById(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Invoice> getByCompany(UUID companyId) {
        return invoiceRepository.findByCompanyId(companyId);
    }
}

package com.crm.api.invoice;

import com.crm.api.shared.ApiResponse;
import com.crm.domain.invoice.Invoice;
import com.crm.domain.invoice.InvoiceStatus;
import com.crm.service.invoice.InvoiceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    record CreateRequest(
            @NotNull UUID companyId,
            @NotBlank String invoiceNumber,
            @NotNull LocalDate issuedDate,
            @NotNull LocalDate dueDate,
            String notes
    ) {}

    record AddLineItemRequest(
            @NotBlank String description,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @Positive BigDecimal unitPrice,
            String serviceCategory
    ) {}

    record InvoiceDto(UUID id, UUID companyId, String invoiceNumber,
                      LocalDate issuedDate, LocalDate dueDate, LocalDate paidDate,
                      InvoiceStatus status, BigDecimal totalAmount,
                      String notes, LocalDateTime createdAt) {
        static InvoiceDto from(Invoice i) {
            return new InvoiceDto(i.getId(), i.getCompany().getId(), i.getInvoiceNumber(),
                    i.getIssuedDate(), i.getDueDate(), i.getPaidDate(),
                    i.getStatus(), i.getTotalAmount(), i.getNotes(), i.getCreatedAt());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvoiceDto> create(@Valid @RequestBody CreateRequest req) {
        Invoice inv = invoiceService.create(req.companyId(), req.invoiceNumber(),
                req.issuedDate(), req.dueDate(), req.notes());
        return ApiResponse.ok(InvoiceDto.from(inv));
    }

    @PostMapping("/{id}/line-items")
    public ApiResponse<InvoiceDto> addLineItem(@PathVariable UUID id,
                                               @Valid @RequestBody AddLineItemRequest req) {
        Invoice inv = invoiceService.addLineItem(id, req.description(),
                req.quantity(), req.unitPrice(), req.serviceCategory());
        return ApiResponse.ok(InvoiceDto.from(inv));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<InvoiceDto> pay(@PathVariable UUID id,
                                       @RequestParam(required = false) LocalDate paidDate) {
        Invoice inv = invoiceService.markPaid(id, paidDate != null ? paidDate : LocalDate.now());
        return ApiResponse.ok(InvoiceDto.from(inv));
    }

    @GetMapping("/company/{companyId}")
    public ApiResponse<List<InvoiceDto>> listByCompany(@PathVariable UUID companyId) {
        return ApiResponse.ok(invoiceService.getByCompany(companyId)
                .stream().map(InvoiceDto::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<InvoiceDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(InvoiceDto.from(invoiceService.getById(id)));
    }
}

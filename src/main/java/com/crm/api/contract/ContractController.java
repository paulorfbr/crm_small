package com.crm.api.contract;

import com.crm.api.shared.ApiResponse;
import com.crm.domain.contract.*;
import com.crm.service.contract.ContractService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    record CreateRequest(
            @NotNull UUID companyId,
            @NotNull ServiceType serviceType,
            @NotNull @Positive BigDecimal monthlyValue,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            String description,
            boolean autoRenews
    ) {}

    record AddPeriodRequest(@NotNull LocalDate periodStart, @NotNull LocalDate periodEnd) {}

    record ContractDto(UUID id, UUID companyId, ServiceType serviceType,
                       String description, BigDecimal monthlyValue,
                       LocalDate startDate, LocalDate endDate,
                       ContractStatus status, boolean autoRenews,
                       LocalDateTime createdAt) {
        static ContractDto from(Contract c) {
            return new ContractDto(c.getId(), c.getCompany().getId(), c.getServiceType(),
                    c.getDescription(), c.getMonthlyValue(), c.getStartDate(), c.getEndDate(),
                    c.getStatus(), c.isAutoRenews(), c.getCreatedAt());
        }
    }

    record PeriodDto(UUID id, UUID contractId, LocalDate periodStart, LocalDate periodEnd,
                     BigDecimal amountBilled, ContractPeriodStatus status) {
        static PeriodDto from(ContractPeriod p) {
            return new PeriodDto(p.getId(), p.getContract().getId(),
                    p.getPeriodStart(), p.getPeriodEnd(),
                    p.getAmountBilled(), p.getStatus());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContractDto> create(@Valid @RequestBody CreateRequest req) {
        Contract c = contractService.create(req.companyId(), req.serviceType(),
                req.monthlyValue(), req.startDate(), req.endDate(),
                req.description(), req.autoRenews());
        return ApiResponse.ok(ContractDto.from(c));
    }

    @GetMapping("/company/{companyId}")
    public ApiResponse<List<ContractDto>> listByCompany(@PathVariable UUID companyId) {
        return ApiResponse.ok(contractService.getByCompany(companyId)
                .stream().map(ContractDto::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ContractDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(ContractDto.from(contractService.getById(id)));
    }

    @PostMapping("/{id}/periods")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PeriodDto> addPeriod(@PathVariable UUID id,
                                             @Valid @RequestBody AddPeriodRequest req) {
        ContractPeriod period = contractService.addPeriod(id, req.periodStart(), req.periodEnd());
        return ApiResponse.ok(PeriodDto.from(period));
    }

    @PostMapping("/periods/{periodId}/pay")
    public ApiResponse<PeriodDto> payPeriod(@PathVariable UUID periodId) {
        ContractPeriod period = contractService.markPeriodPaid(periodId);
        return ApiResponse.ok(PeriodDto.from(period));
    }

    @GetMapping("/{id}/periods")
    public ApiResponse<List<PeriodDto>> listPeriods(@PathVariable UUID id) {
        return ApiResponse.ok(contractService.getPeriodsForContract(id)
                .stream().map(PeriodDto::from).toList());
    }
}

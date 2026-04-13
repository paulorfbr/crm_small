package com.crm.api.company;

import com.crm.api.shared.ApiResponse;
import com.crm.domain.company.Company;
import com.crm.domain.company.CompanyStatus;
import com.crm.domain.company.CompanyTier;
import com.crm.domain.interaction.Interaction;
import com.crm.domain.interaction.InteractionType;
import com.crm.service.company.CompanyService;
import com.crm.service.interaction.InteractionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final InteractionService interactionService;

    public CompanyController(CompanyService companyService,
                             InteractionService interactionService) {
        this.companyService = companyService;
        this.interactionService = interactionService;
    }

    record CreateRequest(
            @NotBlank String name,
            @NotNull CompanyTier tier,
            String industry,
            String region
    ) {}

    record UpdateRequest(
            @NotBlank String name,
            @NotNull CompanyTier tier,
            String industry,
            String region,
            @NotNull CompanyStatus status
    ) {}

    record CompanyDto(UUID id, String name, String industry, String region,
                      CompanyTier tier, CompanyStatus status,
                      LocalDate acquiredDate, LocalDateTime createdAt) {
        static CompanyDto from(Company c) {
            return new CompanyDto(c.getId(), c.getName(), c.getIndustry(), c.getRegion(),
                    c.getTier(), c.getStatus(), c.getAcquiredDate(), c.getCreatedAt());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyDto> create(@Valid @RequestBody CreateRequest req) {
        Company c = companyService.create(req.name(), req.tier(), req.industry(), req.region());
        return ApiResponse.ok(CompanyDto.from(c));
    }

    @GetMapping
    public ApiResponse<List<CompanyDto>> list(
            @RequestParam(required = false) CompanyStatus status) {
        List<Company> companies = status != null
                ? companyService.getByStatus(status)
                : companyService.getAll();
        return ApiResponse.ok(companies.stream().map(CompanyDto::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<CompanyDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(CompanyDto.from(companyService.getById(id)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CompanyDto> update(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateRequest req) {
        Company c = companyService.update(id, req.name(), req.tier(),
                req.industry(), req.region(), req.status());
        return ApiResponse.ok(CompanyDto.from(c));
    }

    @PostMapping("/{id}/churn")
    public ApiResponse<Void> churn(@PathVariable UUID id) {
        companyService.markChurned(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        companyService.delete(id);
    }

    // --- Interaction endpoints ---

    record LogInteractionRequest(
            @NotNull InteractionType interactionType,
            @NotNull LocalDateTime interactedAt,
            String notes,
            String outcome,
            @NotBlank String createdBy,
            UUID contactId
    ) {}

    record InteractionDto(UUID id, UUID companyId, UUID contactId,
                          InteractionType interactionType, LocalDateTime interactedAt,
                          String notes, String outcome, String createdBy,
                          LocalDateTime createdAt) {
        static InteractionDto from(Interaction i) {
            return new InteractionDto(
                    i.getId(), i.getCompany().getId(),
                    i.getContact() != null ? i.getContact().getId() : null,
                    i.getType(), i.getOccurredAt(),
                    i.getNotes(), i.getOutcome(), i.getCreatedByUser(),
                    i.getCreatedAt());
        }
    }

    @PostMapping("/{id}/interactions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InteractionDto> logInteraction(
            @PathVariable UUID id,
            @Valid @RequestBody LogInteractionRequest req) {
        Interaction interaction = interactionService.log(
                id, req.contactId(), req.interactionType(),
                req.interactedAt(), req.notes(), req.outcome(), req.createdBy());
        return ApiResponse.ok(InteractionDto.from(interaction));
    }

    @GetMapping("/{id}/interactions")
    public ApiResponse<List<InteractionDto>> listInteractions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(interactionService.getRecentByCompany(id, limit)
                .stream().map(InteractionDto::from).toList());
    }
}

package com.crm.api.analytics;

import com.crm.api.shared.ApiResponse;
import com.crm.domain.analytics.*;
import com.crm.repository.*;
import com.crm.service.analytics.BcgCalculationService;
import com.crm.service.analytics.LtvCalculationService;
import com.crm.service.analytics.RfmCalculationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final CompanyRfmScoreRepository rfmRepo;
    private final CompanyLtvRecordRepository ltvRepo;
    private final CompanyBcgRecordRepository bcgRepo;
    private final RfmCalculationService rfmService;
    private final LtvCalculationService ltvService;
    private final BcgCalculationService bcgService;

    public AnalyticsController(CompanyRfmScoreRepository rfmRepo,
                                CompanyLtvRecordRepository ltvRepo,
                                CompanyBcgRecordRepository bcgRepo,
                                RfmCalculationService rfmService,
                                LtvCalculationService ltvService,
                                BcgCalculationService bcgService) {
        this.rfmRepo = rfmRepo;
        this.ltvRepo = ltvRepo;
        this.bcgRepo = bcgRepo;
        this.rfmService = rfmService;
        this.ltvService = ltvService;
        this.bcgService = bcgService;
    }

    // --- DTOs ---

    record RfmDto(UUID companyId, String companyName,
                  int recencyDays, short recencyScore,
                  int frequencyCount, short frequencyScore,
                  BigDecimal monetaryTotal, short monetaryScore,
                  short rfmScore, RfmSegment segment, LocalDateTime calculatedAt) {
        static RfmDto from(CompanyRfmScore s) {
            return new RfmDto(s.getCompany().getId(), s.getCompany().getName(),
                    s.getRecencyDays(), s.getRecencyScore(),
                    s.getFrequencyCount(), s.getFrequencyScore(),
                    s.getMonetaryTotal(), s.getMonetaryScore(),
                    s.getRfmScore(), s.getSegment(), s.getCalculatedAt());
        }
    }

    record LtvDto(UUID companyId, String companyName,
                  BigDecimal historicalRevenue, BigDecimal projectedAnnualValue,
                  BigDecimal projectedLtv, BigDecimal avgMonthlyRevenue,
                  ChurnRisk estimatedChurnRisk, int contractMonthsRemaining,
                  LocalDateTime calculatedAt) {
        static LtvDto from(CompanyLtvRecord r) {
            return new LtvDto(r.getCompany().getId(), r.getCompany().getName(),
                    r.getHistoricalRevenue(), r.getProjectedAnnualValue(),
                    r.getProjectedLtv(), r.getAvgMonthlyRevenue(),
                    r.getEstimatedChurnRisk(), r.getContractMonthsRemaining(),
                    r.getCalculatedAt());
        }
    }

    record BcgDto(UUID companyId, String companyName,
                  BigDecimal revenueSharePct, BigDecimal revenueGrowthRatePct,
                  BigDecimal currentYearRevenue, BigDecimal previousYearRevenue,
                  BcgQuadrant quadrant, LocalDateTime calculatedAt) {
        static BcgDto from(CompanyBcgRecord b) {
            return new BcgDto(b.getCompany().getId(), b.getCompany().getName(),
                    b.getRevenueSharePct(), b.getRevenueGrowthRatePct(),
                    b.getCurrentYearRevenue(), b.getPreviousYearRevenue(),
                    b.getQuadrant(), b.getCalculatedAt());
        }
    }

    record SegmentSummaryDto(String segment, long count) {}

    // --- RFM endpoints ---

    @GetMapping("/rfm")
    public ApiResponse<List<RfmDto>> allRfm() {
        return ApiResponse.ok(rfmRepo.findAll().stream().map(RfmDto::from).toList());
    }

    @GetMapping("/rfm/company/{companyId}")
    public ApiResponse<RfmDto> rfmForCompany(@PathVariable UUID companyId) {
        return rfmRepo.findByCompanyId(companyId)
                .map(s -> ApiResponse.ok(RfmDto.from(s)))
                .orElse(ApiResponse.error("No RFM score found for company " + companyId));
    }

    @GetMapping("/rfm/segments")
    public ApiResponse<List<SegmentSummaryDto>> rfmSegmentSummary() {
        return ApiResponse.ok(rfmRepo.countBySegment().stream()
                .map(row -> new SegmentSummaryDto(row[0].toString(), ((Number) row[1]).longValue()))
                .toList());
    }

    @GetMapping("/rfm/at-risk")
    public ApiResponse<List<RfmDto>> atRiskHighValue() {
        return ApiResponse.ok(rfmRepo.findAtRiskHighValue().stream().map(RfmDto::from).toList());
    }

    // --- LTV endpoints ---

    @GetMapping("/ltv/top")
    public ApiResponse<List<LtvDto>> topByLtv(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(ltvRepo.findTopByProjectedLtv(PageRequest.of(0, limit))
                .stream().map(LtvDto::from).toList());
    }

    @GetMapping("/ltv/company/{companyId}")
    public ApiResponse<LtvDto> ltvForCompany(@PathVariable UUID companyId) {
        return ltvRepo.findByCompanyId(companyId)
                .map(r -> ApiResponse.ok(LtvDto.from(r)))
                .orElse(ApiResponse.error("No LTV record found for company " + companyId));
    }

    // --- BCG endpoints ---

    @GetMapping("/bcg")
    public ApiResponse<List<BcgDto>> allBcg() {
        return ApiResponse.ok(bcgRepo.findAll().stream().map(BcgDto::from).toList());
    }

    @GetMapping("/bcg/company/{companyId}")
    public ApiResponse<BcgDto> bcgForCompany(@PathVariable UUID companyId) {
        return bcgRepo.findByCompanyId(companyId)
                .map(b -> ApiResponse.ok(BcgDto.from(b)))
                .orElse(ApiResponse.error("No BCG record found for company " + companyId));
    }

    @GetMapping("/bcg/quadrant/{quadrant}")
    public ApiResponse<List<BcgDto>> bcgByQuadrant(@PathVariable BcgQuadrant quadrant) {
        return ApiResponse.ok(bcgRepo.findByQuadrant(quadrant).stream().map(BcgDto::from).toList());
    }

    @GetMapping("/bcg/summary")
    public ApiResponse<List<SegmentSummaryDto>> bcgSummary() {
        return ApiResponse.ok(bcgRepo.countByQuadrant().stream()
                .map(row -> new SegmentSummaryDto(row[0].toString(), ((Number) row[1]).longValue()))
                .toList());
    }

    // --- Manual trigger (useful during development) ---

    @PostMapping("/recalculate")
    public ApiResponse<String> triggerRecalculation() {
        rfmService.recalculateAll();
        ltvService.recalculateAll();
        bcgService.recalculateAll();
        return ApiResponse.ok("Recalculation complete");
    }
}

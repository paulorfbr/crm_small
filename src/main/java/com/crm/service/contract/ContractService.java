package com.crm.service.contract;

import com.crm.domain.company.Company;
import com.crm.domain.contract.*;
import com.crm.repository.CompanyRepository;
import com.crm.repository.ContractPeriodRepository;
import com.crm.repository.ContractRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractPeriodRepository contractPeriodRepository;
    private final CompanyRepository companyRepository;

    public ContractService(ContractRepository contractRepository,
                           ContractPeriodRepository contractPeriodRepository,
                           CompanyRepository companyRepository) {
        this.contractRepository = contractRepository;
        this.contractPeriodRepository = contractPeriodRepository;
        this.companyRepository = companyRepository;
    }

    public Contract create(UUID companyId, ServiceType serviceType,
                           BigDecimal monthlyValue, LocalDate startDate,
                           LocalDate endDate, String description, boolean autoRenews) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
        Contract contract = new Contract(company, serviceType, monthlyValue, startDate);
        contract.setEndDate(endDate);
        contract.setDescription(description);
        contract.setAutoRenews(autoRenews);
        return contractRepository.save(contract);
    }

    public ContractPeriod addPeriod(UUID contractId,
                                    LocalDate periodStart, LocalDate periodEnd) {
        Contract contract = getById(contractId);
        ContractPeriod period = new ContractPeriod(contract, contract.getCompany(),
                periodStart, periodEnd, contract.getMonthlyValue());
        return contractPeriodRepository.save(period);
    }

    public ContractPeriod markPeriodPaid(UUID periodId) {
        ContractPeriod period = contractPeriodRepository.findById(periodId)
                .orElseThrow(() -> new EntityNotFoundException("ContractPeriod not found: " + periodId));
        period.markPaid();
        return contractPeriodRepository.save(period);
    }

    public Contract updateStatus(UUID contractId, ContractStatus status) {
        Contract contract = getById(contractId);
        contract.setStatus(status);
        return contractRepository.save(contract);
    }

    @Transactional(readOnly = true)
    public Contract getById(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Contract> getByCompany(UUID companyId) {
        return contractRepository.findByCompanyIdAndStatus(companyId, ContractStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<ContractPeriod> getPeriodsForContract(UUID contractId) {
        return contractPeriodRepository.findByContractId(contractId);
    }
}

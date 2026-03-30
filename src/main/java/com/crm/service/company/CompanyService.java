package com.crm.service.company;

import com.crm.domain.company.Company;
import com.crm.domain.company.CompanyStatus;
import com.crm.domain.company.CompanyTier;
import com.crm.repository.CompanyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company create(String name, CompanyTier tier, String industry, String region) {
        Company company = new Company(name, tier);
        company.setIndustry(industry);
        company.setRegion(region);
        return companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public Company getById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Company> getAll() {
        return companyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Company> getByStatus(CompanyStatus status) {
        return companyRepository.findByStatus(status);
    }

    public Company update(UUID id, String name, CompanyTier tier,
                          String industry, String region, CompanyStatus status) {
        Company company = getById(id);
        company.setName(name);
        company.setTier(tier);
        company.setIndustry(industry);
        company.setRegion(region);
        company.setStatus(status);
        return companyRepository.save(company);
    }

    public void markChurned(UUID id) {
        Company company = getById(id);
        company.setStatus(CompanyStatus.CHURNED);
        company.setChurnedDate(LocalDate.now());
        companyRepository.save(company);
    }

    public void delete(UUID id) {
        companyRepository.deleteById(id);
    }
}

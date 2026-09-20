package com.example.nexuscore.service;

import com.example.nexuscore.dto.company.CompanyResponse;
import com.example.nexuscore.dto.company.PlanChangeRequest;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Plan;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.repository.CompanyRepository;
import com.example.nexuscore.repository.PlanRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companies;
    private final PlanRepository plans;
    private final ProfileRepository profiles;
    private final StoreRepository stores;

    public CompanyService(CompanyRepository companies, PlanRepository plans,
                          ProfileRepository profiles, StoreRepository stores) {
        this.companies = companies;
        this.plans = plans;
        this.profiles = profiles;
        this.stores = stores;
    }

    @Transactional
    public CompanyResponse changePlan(Integer profileId, PlanChangeRequest request) {
        Company company = companyOf(profileId);
        Plan plan = plans.findById(request.planId())
                .orElseThrow(() -> new NotFoundException("Plano nao encontrado: " + request.planId()));
        long storeCount = stores.countByCompanyId(company.getId());
        if (storeCount > plan.getStoreLimit()) {
            throw new IllegalArgumentException(
                    "Plano nao comporta as " + storeCount + " lojas da empresa");
        }
        company.setPlan(plan);
        return toResponse(companies.save(company));
    }

    public Company companyOf(Integer profileId) {
        Profile profile = profiles.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Perfil nao encontrado: " + profileId));
        if (profile.getType() != ProfileType.COMPANY) {
            throw new ForbiddenException("Somente perfis COMPANY podem gerenciar a empresa");
        }
        return companies.findByProfileId(profileId)
                .orElseThrow(() -> new NotFoundException("Empresa nao encontrada para o perfil autenticado"));
    }

    private CompanyResponse toResponse(Company company) {
        Plan plan = company.getPlan();
        return new CompanyResponse(
                company.getId(), company.getCnpj(), plan.getId(), plan.getPlanName(),
                plan.getPlanPrice(), plan.getStoreLimit(), stores.countByCompanyId(company.getId()));
    }
}

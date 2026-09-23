package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.company.CompanyResponse;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Plan;

public final class CompanyMapper {

    private CompanyMapper() {
    }

    public static CompanyResponse toResponse(Company company, long storeCount) {
        Plan plan = company.getPlan();

        return new CompanyResponse(
                company.getId(),
                company.getCnpj(),
                plan.getId(),
                plan.getPlanName(),
                plan.getPlanPrice(),
                plan.getStoreLimit(),
                storeCount);
    }
}
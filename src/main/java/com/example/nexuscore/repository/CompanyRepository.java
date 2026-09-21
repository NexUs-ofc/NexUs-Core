package com.example.nexuscore.repository;

import com.example.nexuscore.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Integer> {

    Optional<Company> findByProfileId(Integer profileId);
}

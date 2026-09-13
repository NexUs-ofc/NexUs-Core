package com.example.nexuscore.repository;

import com.example.nexuscore.model.Company;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Integer> {

    Optional<Company> findByProfileId(Integer profileId);
}

package com.example.nexuscore.repository;

import com.example.nexuscore.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Integer> {
}

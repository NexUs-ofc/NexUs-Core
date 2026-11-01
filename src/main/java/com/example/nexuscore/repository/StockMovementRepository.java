package com.example.nexuscore.repository;

import com.example.nexuscore.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {
}

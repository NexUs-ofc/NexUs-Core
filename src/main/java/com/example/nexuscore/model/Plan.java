package com.example.nexuscore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "plan")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "plan_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal planPrice;

    @Column(name = "plan_name", nullable = false, unique = true, length = 80)
    private String planName;

    @Column(name = "store_limit", nullable = false)
    private Integer storeLimit;

    protected Plan() {}

    public Integer getId() { return id; }
    public BigDecimal getPlanPrice() { return planPrice; }
    public String getPlanName() { return planName; }
    public Integer getStoreLimit() { return storeLimit; }
}

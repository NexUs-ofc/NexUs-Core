package com.example.nexuscore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String neighborhood;

    @Column(nullable = false, length = 150)
    private String street;

    @Column(nullable = false, length = 10)
    private String number;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false, length = 100)
    private String city;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 2)
    private String state;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    protected Address() {}

    public Integer getId() { return id; }
    public String getNeighborhood() { return neighborhood; }
    public String getStreet() { return street; }
    public String getNumber() { return number; }
    public String getCep() { return cep; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
}

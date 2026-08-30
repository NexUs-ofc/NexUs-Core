package com.example.nexuscore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "food")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "product_brand", length = 100)
    private String productBrand;

    @Column(name = "package_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal packageQuantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "unit_of_measure", nullable = false, columnDefinition = "unit_of_measure_enum")
    private UnitOfMeasure unitOfMeasure;

    protected Food() {}

    public Integer getId() { return id; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public String getProductBrand() { return productBrand; }
    public BigDecimal getPackageQuantity() { return packageQuantity; }
    public UnitOfMeasure getUnitOfMeasure() { return unitOfMeasure; }
}

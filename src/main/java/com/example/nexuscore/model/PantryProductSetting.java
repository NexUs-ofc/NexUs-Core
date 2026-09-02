package com.example.nexuscore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "pantry_product_setting", uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "food_id"}))
public class PantryProductSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "minimum_quantity", nullable = false)
    private Integer minimumQuantity;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected PantryProductSetting() {
    }

    public PantryProductSetting(Food food, Profile profile, Integer minimumQuantity) {
        this.food = food;
        this.profile = profile;
        this.minimumQuantity = minimumQuantity;
    }

    public Integer getId() {
        return id;
    }
    public Food getFood() {
        return food;
    }
    public Profile getProfile() {
        return profile;
    }
    public Integer getMinimumQuantity() {
        return minimumQuantity;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setMinimumQuantity(Integer minimumQuantity) {
        this.minimumQuantity = minimumQuantity;
    }
}

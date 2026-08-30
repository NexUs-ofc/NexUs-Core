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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pantry_item")
public class PantryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PantryItem() {}

    public PantryItem(Food food, Profile profile, Integer quantity, LocalDate expiryDate) {
        this.food = food;
        this.profile = profile;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
    }

    public Integer getId() { return id; }
    public Food getFood() { return food; }
    public Profile getProfile() { return profile; }
    public Integer getQuantity() { return quantity; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}

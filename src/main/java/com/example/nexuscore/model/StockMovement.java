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
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_estoque", schema = "bi_hist")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pantry_item_id", nullable = false, updatable = false)
    private PantryItem pantryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30, updatable = false)
    private StockMovementType type;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @Column(name = "event_date", nullable = false, updatable = false)
    private LocalDateTime eventDate;

    protected StockMovement() {
    }

    public StockMovement(PantryItem pantryItem, StockMovementType type, Integer quantity) {
        this.pantryItem = pantryItem;
        this.type = type;
        this.quantity = quantity;
        this.eventDate = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public PantryItem getPantryItem() {
        return pantryItem;
    }

    public StockMovementType getType() {
        return type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }
}

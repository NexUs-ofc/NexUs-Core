package com.example.nexuscore.model;

import java.util.UUID;

public class ShoppingListItem {

    private String id;
    private Integer foodId;
    private String name;
    private Double quantity;
    private String unitOfMeasure;
    private boolean checked;

    protected ShoppingListItem() {}

    public ShoppingListItem(Integer foodId, String name, Double quantity, String unitOfMeasure, boolean checked) {
        this.id = UUID.randomUUID().toString();
        this.foodId = foodId;
        this.name = name;
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure;
        this.checked = checked;
    }

    public String getId() { return id; }
    public Integer getFoodId() { return foodId; }
    public String getName() { return name; }
    public Double getQuantity() { return quantity; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public boolean isChecked() { return checked; }

    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public void setChecked(boolean checked) { this.checked = checked; }
}

package com.example.nexuscore.model;

import org.springframework.data.mongodb.core.mapping.Field;

public class EventRecipeIngredient {

    @Field("food_id")
    private Integer foodId;

    private String ingredient;

    @Field("total_quantity")
    private String totalQuantity;

    @Field("has_enough")
    private Boolean hasEnough;

    protected EventRecipeIngredient() {
    }

    public Integer getFoodId() {
        return foodId;
    }

    public String getIngredient() {
        return ingredient;
    }

    public String getTotalQuantity() {
        return totalQuantity;
    }

    public Boolean getHasEnough() {
        return hasEnough;
    }
}

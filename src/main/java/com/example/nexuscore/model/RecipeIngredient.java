package com.example.nexuscore.model;

import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

public class RecipeIngredient {

    @Field("food_id")
    private Integer foodId;

    @Field("required_quantity")
    private String requiredQuantity;

    private Boolean mandatory;

    @Field("possible_substitutes")
    private List<Integer> possibleSubstitutes;

    protected RecipeIngredient() {
    }

    public Integer getFoodId() {
        return foodId;
    }

    public String getRequiredQuantity() {
        return requiredQuantity;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public List<Integer> getPossibleSubstitutes() {
        return possibleSubstitutes;
    }
}

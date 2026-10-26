package com.example.nexuscore.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "shopping_list")
public class ShoppingList {

    @Id
    private String id;

    @Indexed(name = "idx_shopping_list_household_id")
    @Field("household_id")
    private Integer householdId;

    private String title;

    @Field(write = Field.Write.NON_NULL)
    private LocalDate date;

    @Field("array_list")
    private List<ShoppingListItem> arrayList = new ArrayList<>();

    protected ShoppingList() {
    }

    public ShoppingList(Integer householdId, String title, LocalDate date) {
        this.householdId = householdId;
        this.title = title;
        this.date = date;
    }

    public String getId() {
        return id;
    }
    public Integer getHouseholdId() {
        return householdId;
    }
    public String getTitle() {
        return title;
    }
    public LocalDate getDate() {
        return date;
    }
    public List<ShoppingListItem> getArrayList() {
        return arrayList;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}

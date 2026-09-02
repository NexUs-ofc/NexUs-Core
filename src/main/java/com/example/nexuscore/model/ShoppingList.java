package com.example.nexuscore.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "shopping_list")
public class ShoppingList {

    @Id
    private String id;

    @Field("household_id")
    private Integer householdId;

    private String title;

    @Field("event_id")
    private String eventId;

    @Field("array_list")
    private List<ShoppingListItem> arrayList = new ArrayList<>();

    protected ShoppingList() {
    }

    public ShoppingList(Integer householdId, String title, String eventId) {
        this.householdId = householdId;
        this.title = title;
        this.eventId = eventId;
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
    public String getEventId() {
        return eventId;
    }
    public List<ShoppingListItem> getArrayList() {
        return arrayList;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

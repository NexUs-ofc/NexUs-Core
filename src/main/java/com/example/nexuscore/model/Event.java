package com.example.nexuscore.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "events")
@CompoundIndexes({
    @CompoundIndex(name = "idx_events_household_date", def = "{'household_id': 1, 'date': 1}"),
    @CompoundIndex(name = "idx_events_date_id", def = "{'date': 1, '_id': 1}")
})
public class Event {

    @Id
    private ObjectId id;

    @Field("household_id")
    private Integer householdId;

    private String title;

    private String description;

    private LocalDateTime date;

    private Integer duration;

    @Field("local")
    private String location;

    @Field("qtd_people")
    private Integer peopleCount;

    private List<EventRecipe> recipes = new ArrayList<>();

    protected Event() {
    }

    public Event(Integer householdId, String title, String description, LocalDateTime date,
                 Integer duration, String location, Integer peopleCount) {
        this.householdId = householdId;
        this.title = title;
        this.description = description;
        this.date = date;
        this.duration = duration;
        this.location = location;
        this.peopleCount = peopleCount;
    }

    public ObjectId getId() {
        return id;
    }

    public Integer getHouseholdId() {
        return householdId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getLocation() {
        return location;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public List<EventRecipe> getRecipes() {
        return recipes;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }
}

package com.example.nexuscore.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "event")
public class Event {

    @Id
    private String id;

    @Field("household_id")
    private Integer householdId;

    private String title;

    private String description;

    private LocalDateTime date;

    private Integer duration;

    private String location;

    @Field("people_count")
    private Integer peopleCount;

    private List<EventRecipe> recipes = new ArrayList<>();

    protected Event() {}

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

    public String getId() { return id; }
    public Integer getHouseholdId() { return householdId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getDate() { return date; }
    public Integer getDuration() { return duration; }
    public String getLocation() { return location; }
    public Integer getPeopleCount() { return peopleCount; }
    public List<EventRecipe> getRecipes() { return recipes; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public void setLocation(String location) { this.location = location; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
}

package com.example.nexuscore.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;
import java.util.List;

@Document(collection = "profile_dietary_restrictions")
public class ProfileDietaryRestrictions {

    @Id
    private Integer profileId;

    @Field("dietary_restrictions")
    private List<String> dietaryRestrictions;

    @Field("updated_at")
    private Instant updatedAt;

    protected ProfileDietaryRestrictions() {
    }

    public ProfileDietaryRestrictions(Integer profileId, List<String> dietaryRestrictions) {
        this.profileId = profileId;
        this.dietaryRestrictions = List.copyOf(dietaryRestrictions);
        this.updatedAt = Instant.now();
    }

    public Integer getProfileId() {
        return profileId;
    }

    public List<String> getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

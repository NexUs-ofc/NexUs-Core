package com.example.nexuscore.repository;

import com.example.nexuscore.model.ProfileDietaryRestrictions;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProfileDietaryRestrictionsRepository extends MongoRepository<ProfileDietaryRestrictions, Integer> {
}

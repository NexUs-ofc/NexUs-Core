package com.example.nexuscore.repository;

import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface ProfileRepository extends JpaRepository<Profile, Integer> {

    boolean existsByEmail(String email);

    List<Profile> findByIdInAndStatus(Collection<Integer> ids, ProfileStatus status);
}

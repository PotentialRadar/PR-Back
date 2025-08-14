package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUser(User user);
}

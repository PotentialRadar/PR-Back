package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmail(String email);
    void deleteByEmail(String email);
}

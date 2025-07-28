package com.potential_radar.PR.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Entity
@Table(name="email_verification")
@Getter
@NoArgsConstructor
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String email;

    private String code;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    public EmailVerification(String email, String code) {
        this.email = email;
        this.code = code;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return createdAt.isBefore(LocalDateTime.now().minusMinutes(3));
    }
}

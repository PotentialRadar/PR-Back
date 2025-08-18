package com.potential_radar.PR.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", updatable = false)
    private Long id;

    @Column(name="user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name="refresh_token", nullable = false)
    private String refreshToken;

    @Column(name="expiry_date", nullable = false)
    private Instant expiryDate;

    public RefreshToken(Long userId, String refreshToken, Instant expiryDate) {
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.expiryDate = expiryDate;
    }

    public RefreshToken update(String newRefreshToken, Instant newExpiryDate) {
        this.refreshToken = newRefreshToken;
        this.expiryDate = newExpiryDate;
        return this;
    }

    public boolean isExpired(){

        return this.expiryDate.isBefore(Instant.now());
    }
}

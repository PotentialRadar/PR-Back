package com.potential_radar.PR.user.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uk_users_nickname", columnNames = {"nickname"}),
                @UniqueConstraint(name = "uk_users_provider_providerUserId", columnNames = {"provider","provider_user_id"})
        }
)
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // citext 매핑: columnDefinition 으로 지정 (Hibernate는 String으로 처리 가능)
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column
    private String password; // 소셜 로그인은 null 가능

    @Column(nullable = false, columnDefinition = "citext")
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider; // EMAIL, GOOGLE, KAKAO

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(nullable = false)
    @ColumnDefault("now()")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @ColumnDefault("now()")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public User(String email, String password, String nickname,
                Provider provider, String providerUserId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }
}

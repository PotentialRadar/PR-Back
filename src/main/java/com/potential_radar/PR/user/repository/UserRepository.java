package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.model.Provider;
import com.potential_radar.PR.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);


    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByProviderAndProviderUserId(Provider provider, String providerUserId);

    @Modifying
    @Query("UPDATE User u SET u.nickname = :nickname, u.email = :email, u.updatedAt = CURRENT_TIMESTAMP WHERE u.userId = :userId")
    void updateUserBasic(Long userId, String nickname, String email);

    @Modifying
    @Query("UPDATE User u SET u.nickname = :nickname, u.updatedAt = CURRENT_TIMESTAMP WHERE u.userId = :userId")
    void updateNickname(Long userId, String nickname);

}

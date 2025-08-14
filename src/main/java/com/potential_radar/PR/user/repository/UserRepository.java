package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.model.Provider;
import com.potential_radar.PR.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);


    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByProviderAndProviderUserId(Provider provider, String providerUserId);

}

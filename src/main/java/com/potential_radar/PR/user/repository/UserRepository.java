package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderUserId(User.Provider provider, String providerUserName);

    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    // 증분 동기화를 위한 메서드들
    List<User> findByUpdatedAtAfter(LocalDateTime since);
    
    List<User> findByUpdatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.updatedAt > :since")
    long countByUpdatedAtAfter(@Param("since") LocalDateTime since);
    
    @Query("SELECT u FROM User u WHERE u.updatedAt > :since ORDER BY u.updatedAt ASC")
    List<User> findByUpdatedAtAfterOrderByUpdatedAt(@Param("since") LocalDateTime since);

}

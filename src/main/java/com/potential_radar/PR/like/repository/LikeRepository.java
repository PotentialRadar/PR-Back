package com.potential_radar.PR.like.repository;

import com.potential_radar.PR.like.domain.Like;
import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndTargetTypeAndTargetId(User user, TargetType targetType, Long targetId);

    long countByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    List<Like> findAllByUserAndTargetType(User user, TargetType targetType);

    boolean existsByUserAndTargetTypeAndTargetId(User user, TargetType targetType, Long targetId);
}

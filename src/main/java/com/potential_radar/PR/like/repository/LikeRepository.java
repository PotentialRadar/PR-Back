package com.potential_radar.PR.like.repository;

import com.potential_radar.PR.like.domain.Like;
import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.user.domain.User;

import lombok.NonNull;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndTargetTypeAndTargetId(@NonNull User user, @NonNull TargetType targetType, @NonNull Long targetId);

    long countByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    List<Like> findAllByUserAndTargetType(@NonNull User user, @NonNull TargetType targetType);

    boolean existsByUserAndTargetTypeAndTargetId(@NonNull User user, @NonNull TargetType targetType, @NonNull Long targetId);
}

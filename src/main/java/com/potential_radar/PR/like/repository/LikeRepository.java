package com.potential_radar.PR.like.repository;

import com.potential_radar.PR.like.domain.Like;
import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndTargetTypeAndTargetId(User user, TargetType targetType, Long targetId);
    List<Like> findByUserAndTargetType(User user, TargetType targetType);
    long countByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.targetType = :targetType AND l.targetId = :targetId")
    long countByTargetTypeAndTargetIdCustom(@Param("targetType") TargetType targetType, @Param("targetId") Long targetId);
}

package com.potential_radar.PR.user.dto;

import com.potential_radar.PR.user.domain.User;

public record UserInfoResponse(
        Long id,
        String email,
        String nickname,
        String profileImage
) {
    public UserInfoResponse(User user) {
        this(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage()
        );
    }
}

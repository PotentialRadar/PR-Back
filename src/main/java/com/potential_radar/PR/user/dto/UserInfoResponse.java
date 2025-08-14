package com.potential_radar.PR.user.dto;

import com.potential_radar.PR.user.model.User;

public record UserInfoResponse(
        Long id,
        String email,
        String nickname
) {
    public UserInfoResponse(User user) {
        this(
                user.getUserId(),
                user.getEmail(),
                user.getNickname()
        );
    }
}

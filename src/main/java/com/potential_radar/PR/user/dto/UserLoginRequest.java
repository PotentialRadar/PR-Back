package com.potential_radar.PR.user.dto;

public record UserLoginRequest(
        String email,
        String password
) {
}

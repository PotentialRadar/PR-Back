package com.potential_radar.PR.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccessTokenRequest {
    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}

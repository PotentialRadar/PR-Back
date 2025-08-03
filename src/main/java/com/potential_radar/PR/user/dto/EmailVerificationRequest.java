package com.potential_radar.PR.user.dto;

import lombok.Data;

@Data
public class EmailVerificationRequest {
    private String email;
    private String code;
}

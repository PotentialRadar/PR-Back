package com.potential_radar.PR.common.exception;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private final int status;
    private final String message;

    public ErrorResponse(String message) {
        this.status = 0;
        this.message = message;
    }

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
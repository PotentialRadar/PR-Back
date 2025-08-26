package com.potential_radar.PR.common.exception;

public class InvitationException extends RuntimeException {
    public InvitationException(String message) {
        super(message);
    }
    
    public InvitationException(String message, Throwable cause) {
        super(message, cause);
    }
}
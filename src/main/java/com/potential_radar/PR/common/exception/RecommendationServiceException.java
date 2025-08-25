package com.potential_radar.PR.common.exception;

public class RecommendationServiceException extends RuntimeException {
    public RecommendationServiceException(String message) {
        super(message);
    }
    
    public RecommendationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
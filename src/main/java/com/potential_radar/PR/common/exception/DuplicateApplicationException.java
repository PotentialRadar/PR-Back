package com.potential_radar.PR.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict
public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException(String message) {super(message);
    }
}

package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class DefaultErrorResponse {
    private final String message;

    private DefaultErrorResponse(String message) {
        this.message = message;
    }

    public static DefaultErrorResponse withMessage(String message) {
        Objects.requireNonNull(message, "Parameter message must not be null");
        return new DefaultErrorResponse(message);
    }

    @Override
    public String toString() {
        return "DefaultErrorResponse{" +
                "message='" + message + '\'' +
                '}';
    }
}
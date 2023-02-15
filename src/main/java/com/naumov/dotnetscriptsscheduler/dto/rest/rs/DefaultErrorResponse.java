package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class DefaultErrorResponse {
    private String message;

    @Override
    public String toString() {
        return "DefaultErrorResponse{" +
                "message='" + message + '\'' +
                '}';
    }
}
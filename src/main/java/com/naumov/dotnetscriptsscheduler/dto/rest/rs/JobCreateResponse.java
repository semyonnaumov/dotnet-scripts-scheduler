package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class JobCreateResponse {
    private UUID jobId;

    @Override
    public String toString() {
        return "JobCreateResponse{" +
                "jobId='" + jobId + '\'' +
                '}';
    }
}

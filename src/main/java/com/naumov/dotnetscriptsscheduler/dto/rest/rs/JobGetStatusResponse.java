package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class JobGetStatusResponse {
    private UUID jobId;
    private JobStatus status;

    @Override
    public String toString() {
        return "JobGetStatusResponse{" +
                "jobId=" + jobId +
                ", status=" + status +
                '}';
    }
}

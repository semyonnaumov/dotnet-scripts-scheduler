package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class JobGetResultResponse {
    private UUID jobId;
    private JobResult result;

    @Override
    public String toString() {
        return "JobGetResultResponse{" +
                "jobId=" + jobId +
                ", result=" + result +
                '}';
    }
}

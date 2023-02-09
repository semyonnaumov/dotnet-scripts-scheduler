package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
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

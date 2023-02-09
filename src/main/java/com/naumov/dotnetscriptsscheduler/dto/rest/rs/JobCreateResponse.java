package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class JobCreateResponse {
    private String jobId;

    @Override
    public String toString() {
        return "JobCreateResponse{" +
                "jobId='" + jobId + '\'' +
                '}';
    }
}

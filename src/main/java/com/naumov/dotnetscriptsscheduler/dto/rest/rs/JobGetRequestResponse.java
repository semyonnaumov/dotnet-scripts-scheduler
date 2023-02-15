package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class JobGetRequestResponse {
    private UUID jobId;
    private JobCreateRequest request;

    @Override
    public String toString() {
        return "JobGetRequestResponse{" +
                "jobId=" + jobId +
                ", request=" + request +
                '}';
    }
}

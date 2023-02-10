package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public final class JobGetResponse {
    private UUID jobId;
    private JobCreateRequest request;
    private JobStatus status;
    private JobResult result;

    @Override
    public String toString() {
        return "JobGetResponse{" +
                "jobId=" + jobId +
                ", request=" + request +
                ", status=" + status +
                ", result=" + result +
                '}';
    }
}

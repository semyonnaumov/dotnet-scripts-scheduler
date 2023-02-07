package com.naumov.dotnetscriptsscheduler.dto.rs;

import com.naumov.dotnetscriptsscheduler.dto.rq.JobCreateRequest;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class JobGetResponse {
    private final JobCreateRequest request;
    private final JobStatus status;
    private final JobResult result;
}

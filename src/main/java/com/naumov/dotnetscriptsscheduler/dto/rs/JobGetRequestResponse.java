package com.naumov.dotnetscriptsscheduler.dto.rs;

import com.naumov.dotnetscriptsscheduler.dto.rq.JobCreateRequest;
import lombok.Data;

@Data
public class JobGetRequestResponse {
    private final JobCreateRequest request;
}

package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import lombok.Data;

@Data
public class JobGetRequestResponse {
    private final JobCreateRequest request;
}

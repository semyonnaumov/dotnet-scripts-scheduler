package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.Data;

@Data
public class JobGetStatusResponse {
    private final JobStatus status;
}

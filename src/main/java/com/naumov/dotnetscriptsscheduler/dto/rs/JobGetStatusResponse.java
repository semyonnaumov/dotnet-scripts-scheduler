package com.naumov.dotnetscriptsscheduler.dto.rs;

import lombok.Data;

@Data
public class JobGetStatusResponse {
    private final JobStatus status;
}

package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import lombok.Data;

@Data
public class JobFinishedMessage {
    private String jobId;
    private JobStatus status;
    private ScriptResults scriptResults;
}

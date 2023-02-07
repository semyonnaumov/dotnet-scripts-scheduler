package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobFinishedMessage {
    @NotBlank
    private String jobId;
    @NotNull
    private JobStatus status;
    private ScriptResults scriptResults;
}

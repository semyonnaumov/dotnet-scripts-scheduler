package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class JobFinishedMessage {
    @NotBlank
    private String jobId;
    @NotNull
    private JobStatus status;
    private ScriptResults scriptResults;

    @Override
    public String toString() {
        return "JobFinishedMessage{" +
                "jobId='" + jobId + '\'' +
                ", status=" + status +
                ", scriptResults=" + scriptResults +
                '}';
    }
}

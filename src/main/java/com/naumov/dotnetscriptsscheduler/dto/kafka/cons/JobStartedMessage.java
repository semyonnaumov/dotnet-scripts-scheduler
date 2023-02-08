package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobStartedMessage {
    @NotBlank
    private String jobId;

    @Override
    public String toString() {
        return "JobStartedMessage{" +
                "jobId='" + jobId + '\'' +
                '}';
    }
}

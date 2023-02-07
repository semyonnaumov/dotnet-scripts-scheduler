package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobStartedMessage {
    @NotBlank
    private String jobId;
}

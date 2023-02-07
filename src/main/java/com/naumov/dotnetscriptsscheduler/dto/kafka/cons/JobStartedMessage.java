package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import lombok.Data;

@Data
public class JobStartedMessage {
    private String jobId;
}

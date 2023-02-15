package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import java.util.UUID;

public interface JobMessage {
    UUID getJobId();

    void setJobId(UUID jobId);
}

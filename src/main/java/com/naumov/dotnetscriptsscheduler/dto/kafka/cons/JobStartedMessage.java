package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class JobStartedMessage implements JobMessage {
    @NotNull
    private UUID jobId;

    @Override
    public String toString() {
        return "JobStartedMessage{" +
                "jobId='" + jobId + '\'' +
                '}';
    }
}

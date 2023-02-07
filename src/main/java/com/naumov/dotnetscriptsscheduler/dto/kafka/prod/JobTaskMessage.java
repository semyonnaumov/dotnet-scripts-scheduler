package com.naumov.dotnetscriptsscheduler.dto.kafka.prod;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobTaskMessage {
    private String jobId;
    private String script;
    private JobConfig jobConfig;
}

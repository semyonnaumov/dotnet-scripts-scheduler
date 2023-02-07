package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class JobResult {
    private final JobCompletionStatus finishedWith;
    private final String stdout;
    private final String stderr;
}

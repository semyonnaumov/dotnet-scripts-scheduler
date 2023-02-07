package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScriptResults {
    @NotNull
    private JobCompletionStatus finishedWith;
    private String stdout;
    private String stderr;
}
package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import lombok.Data;

@Data
public class ScriptResults {
    private JobCompletionStatus finishedWith;
    private String stdout;
    private String stderr;
}
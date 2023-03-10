package com.naumov.dotnetscriptsscheduler.dto.kafka.cons;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import static com.naumov.dotnetscriptsscheduler.util.StringUtil.omitLongString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class ScriptResults {
    @NotNull
    private JobCompletionStatus finishedWith;
    private String stdout;
    private String stderr;

    @Override
    public String toString() {
        return "ScriptResults{" +
                "finishedWith=" + finishedWith +
                ", stdout='" + omitLongString(stdout) + '\'' +
                ", stderr='" + omitLongString(stderr) + '\'' +
                '}';
    }
}
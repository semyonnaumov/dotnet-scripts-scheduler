package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.*;

import static com.naumov.dotnetscriptsscheduler.util.StringUtil.omitLongString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class JobResult {
    private JobCompletionStatus finishedWith;
    private String stdout;
    private String stderr;

    @Override
    public String toString() {
        return "JobResult{" +
                "finishedWith=" + finishedWith +
                ", stdout='" + omitLongString(stdout) + '\'' +
                ", stderr='" + omitLongString(stderr) + '\'' +
                '}';
    }
}

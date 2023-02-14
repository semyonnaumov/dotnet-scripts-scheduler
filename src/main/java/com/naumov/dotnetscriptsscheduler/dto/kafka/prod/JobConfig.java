package com.naumov.dotnetscriptsscheduler.dto.kafka.prod;

import lombok.*;

import static com.naumov.dotnetscriptsscheduler.util.StringUtil.omitLongString;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class JobConfig {
    private String nugetConfigXml;

    @Override
    public String toString() {
        return "JobConfig{" +
                "nugetConfigXml='" + omitLongString(nugetConfigXml) + '\'' +
                '}';
    }
}
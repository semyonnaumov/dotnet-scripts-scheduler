package com.naumov.dotnetscriptsscheduler.dto.kafka.prod;

import lombok.Getter;
import lombok.Setter;

import static com.naumov.dotnetscriptsscheduler.util.StringUtil.omitLongString;

@Getter
@Setter
public final class JobConfig {
    private String nugetConfigXml;

    @Override
    public String toString() {
        return "JobConfig{" +
                "nugetConfigXml='" + omitLongString(nugetConfigXml) + '\'' +
                '}';
    }
}
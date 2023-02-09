package com.naumov.dotnetscriptsscheduler.dto.rest.rq;

import lombok.Getter;
import lombok.Setter;

import static com.naumov.dotnetscriptsscheduler.util.StringUtil.omitLongString;

@Getter
@Setter
public final class JobRequestPayloadConfig {
    private String nugetConfigXml;

    @Override
    public String toString() {
        return "JobRequestPayloadConfig{" +
                "nugetConfigXml='" + omitLongString(nugetConfigXml) + '\'' +
                '}';
    }
}
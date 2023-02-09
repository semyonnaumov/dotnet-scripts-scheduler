package com.naumov.dotnetscriptsscheduler.dto.rest.rq;

import lombok.Getter;
import lombok.Setter;

import static com.naumov.dotnetscriptsscheduler.util.StringUtil.omitLongString;

@Getter
@Setter
public final class JobRequestPayload {
    private String script;
    private JobRequestPayloadConfig jobConfig;
    private String agentType;

    @Override
    public String toString() {
        return "JobRequestPayload{" +
                "script='" + omitLongString(script) + '\'' +
                ", jobConfig=" + jobConfig +
                ", agentType='" + agentType + '\'' +
                '}';
    }
}
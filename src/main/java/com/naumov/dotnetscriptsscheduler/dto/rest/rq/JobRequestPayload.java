package com.naumov.dotnetscriptsscheduler.dto.rest.rq;

import lombok.Data;

@Data
public class JobRequestPayload {
    private String script;
    private JobRequestPayloadConfig jobConfig;
    private String agentType;
}
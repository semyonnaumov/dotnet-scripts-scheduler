package com.naumov.dotnetscriptsscheduler.dto.rest.rq;

import lombok.Data;

@Data
public class JobCreateRequest {
    private String requestId;
    private String senderId;
    private JobRequestPayload payload;
}
